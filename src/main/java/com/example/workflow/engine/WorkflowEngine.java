package com.example.workflow.engine;

import com.example.workflow.model.Workflow;
import com.example.workflow.model.WorkflowInputData;
import com.example.workflow.model.WorkflowOutputData;
import com.example.workflow.nodes.BaseWorkflowNode;
import com.example.workflow.nodes.NodeRegistry;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;

/**
 * The core engine responsible for executing workflow nodes asynchronously.
 */
@Component
public class WorkflowEngine {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowEngine.class);
    private static final long NODE_SHUTDOWN_TIMEOUT_MS = 5000;  // 5-second timeout for shutdown

    private final NodeRegistry nodeRegistry;
    private final WorkflowManagerFactory workflowManagerFactory;
    private final ExecutorService executorService;
    private final Map<BaseWorkflowNode, CompletableFuture<WorkflowOutputData>> activeNodes = new ConcurrentHashMap<>();
    private volatile boolean cancelRequested = false;

    private WorkflowManager workflowManager;

    @Autowired
    public WorkflowEngine(NodeRegistry nodeRegistry,
                          WorkflowManagerFactory workflowManagerFactory,
                          ExecutorService executorService) {
        this.nodeRegistry = nodeRegistry;
        this.workflowManagerFactory = workflowManagerFactory;
        this.executorService = executorService;
    }

    public void loadWorkflow(Workflow workflow) {
        if (workflow == null) {
            throw new IllegalArgumentException("Workflow cannot be null.");
        }
        logger.info("Loading new workflow.");
        this.workflowManager = workflowManagerFactory.createWorkflowManager(workflow);
    }

    public void ejectWorkflow() {
        if (this.workflowManager == null) {
            throw new IllegalStateException("No workflow is currently loaded to eject.");
        }
        logger.info("Ejecting current workflow.");
        this.workflowManager = null;
    }

    public CompletableFuture<Void> executeWorkflowAsync() {
        if (this.workflowManager == null) {
            throw new IllegalStateException("No workflow is loaded. Load a workflow before execution.");
        }
        cancelRequested = false;
        logger.info("Starting workflow execution asynchronously.");

        Workflow workflow = workflowManager.getWorkflow();
        return CompletableFuture.runAsync(() -> {
            List<Workflow.NodeDefinition> nodes = workflow.getWorkflow().getNodes();
            // Use common execution logic for both strategies.
            if (workflow.getWorkflow().getExecutionStrategy() == Workflow.ExecutionStrategy.PARALLEL) {
                executeNodesInParallel(nodes);
            } else {
                executeNodesSequentially(nodes);
            }
        }, executorService);
    }

    public void abortWorkflow() {
        if (this.workflowManager == null) {
            throw new IllegalStateException("No workflow is loaded to abort.");
        }
        logger.info("Aborting workflow execution.");
        cancelRequested = true;
        activeNodes.forEach((node, future) -> {
            logger.info("Aborting node: {}", node.getNodeId());
            handleNodeAbort(node, future);
        });
        activeNodes.clear();
    }

    private void executeNodesSequentially(List<Workflow.NodeDefinition> nodes) {
        logger.info("Starting sequential workflow execution.");
        for (Workflow.NodeDefinition nodeDef : nodes) {
            if (cancelRequested) {
                break;
            }
            CompletableFuture<WorkflowOutputData> future = runNode(nodeDef);
            // Handle errors gracefully
            future.handle((result, ex) -> {
                if (ex != null) {
                    String errorMessage = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    logger.error("Error executing node {}: {}", nodeDef.getId(), errorMessage);
                    // Create a failure output data object for this node
                    WorkflowOutputData failedOutput = new WorkflowOutputData(
                            WorkflowOutputData.Status.FAIL,
                            JsonNodeFactory.instance.objectNode().put("error", errorMessage)
                    );
                    failedOutput.setNodeId(nodeDef.getId());
                    // Update the workflow state with the failure
                    workflowManager.updateWorkflow(failedOutput);
                    return failedOutput;
                }
                return result;
            }).join();
        }
        logger.info("Sequential workflow execution completed.");
    }

    private void executeNodesInParallel(List<Workflow.NodeDefinition> nodes) {
        logger.info("Starting parallel workflow execution.");
        List<CompletableFuture<WorkflowOutputData>> futures = new ArrayList<>();
        for (Workflow.NodeDefinition nodeDef : nodes) {
            if (cancelRequested) {
                break;
            }
            CompletableFuture<WorkflowOutputData> future = runNode(nodeDef)
                    .handle((result, ex) -> {
                        if (ex != null) {
                            String errorMessage = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                            logger.error("Error executing node {}: {}", nodeDef.getId(), errorMessage);
                            WorkflowOutputData failedOutput = new WorkflowOutputData(
                                    WorkflowOutputData.Status.FAIL,
                                    JsonNodeFactory.instance.objectNode().put("error", errorMessage)
                            );
                            failedOutput.setNodeId(nodeDef.getId());
                            workflowManager.updateWorkflow(failedOutput);
                            return failedOutput;
                        }
                        return result;
                    });
            futures.add(future);
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        logger.info("Parallel workflow execution completed.");
    }


    /**
     * Helper method to run a node and register its CompletableFuture.
     */
    private CompletableFuture<WorkflowOutputData> runNode(Workflow.NodeDefinition nodeDef) {
        BaseWorkflowNode node = getNodeOrThrow(nodeDef.getType());
        logger.info("Executing node: {} of type: {}", nodeDef.getId(), nodeDef.getType());
        WorkflowInputData inputData = new WorkflowInputData(nodeDef.getConfig(), null);
        CompletableFuture<WorkflowOutputData> future = node.runAsync(nodeDef.getId(), inputData)
                .thenApply(result -> handleNodeCompletion(node, result));
        activeNodes.put(node, future);
        return future;
    }

    private BaseWorkflowNode getNodeOrThrow(String type) {
        BaseWorkflowNode node = nodeRegistry.getNode(type);
        if (node == null) {
            throw new IllegalArgumentException("Unknown node type: " + type);
        }
        return node;
    }

    private WorkflowOutputData handleNodeCompletion(BaseWorkflowNode node, WorkflowOutputData outputData) {
        if (workflowManager == null) {
            throw new IllegalStateException("No workflow is loaded. Cannot update workflow state.");
        }
        workflowManager.updateWorkflow(outputData);
        activeNodes.remove(node);
        return outputData;
    }

    private WorkflowOutputData handleNodeAbort(BaseWorkflowNode node, CompletableFuture<WorkflowOutputData> future) {
        if (workflowManager == null) {
            throw new IllegalStateException("No workflow is loaded. Cannot update workflow state.");
        }
        String nodeId = node.getNodeId();
        logger.info("Shutdown requested for node: {}", nodeId);
        node.abort();

        if (future != null) {
            try {
                WorkflowOutputData outputData = future.get(NODE_SHUTDOWN_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                workflowManager.updateWorkflow(outputData);
                activeNodes.remove(node);
                return outputData;
            } catch (TimeoutException e) {
                logger.warn("Node {} did not shut down within {} ms. Canceling the future.", nodeId, NODE_SHUTDOWN_TIMEOUT_MS);
                future.cancel(true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Shutdown of node {} was interrupted.", nodeId);
                future.cancel(true);
            } catch (ExecutionException e) {
                logger.error("Error while shutting down node {}: {}", nodeId, e.getMessage());
                future.cancel(true);
            }
        }

        activeNodes.remove(node);
        WorkflowOutputData abortedData = new WorkflowOutputData(
                WorkflowOutputData.Status.ABORTED,
                new com.fasterxml.jackson.databind.node.ObjectNode(
                        com.fasterxml.jackson.databind.node.JsonNodeFactory.instance
                ).put("message", "Node " + nodeId + " failed to shut down in time.")
        );
        workflowManager.updateWorkflow(abortedData);
        return abortedData;
    }

}
