package com.example.workflow.engine;

import com.example.workflow.model.Workflow;
import com.example.workflow.model.WorkflowInputData;
import com.example.workflow.model.WorkflowOutputData;
import com.example.workflow.nodes.BaseWorkflowNode;
import com.example.workflow.nodes.NodeRegistry;
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
    private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private final Map<BaseWorkflowNode, CompletableFuture<WorkflowOutputData>> activeNodes = new ConcurrentHashMap<>();
    private volatile boolean cancelRequested = false;

    private WorkflowManager workflowManager;

    @Autowired
    public WorkflowEngine(NodeRegistry nodeRegistry, WorkflowManagerFactory workflowManagerFactory) {
        this.nodeRegistry = nodeRegistry;
        this.workflowManagerFactory = workflowManagerFactory;
    }

    /**
     * Loads a workflow and initializes a WorkflowManager for execution.
     * @param workflow The workflow to load.
     */
    public void loadWorkflow(Workflow workflow) {
        if (workflow == null) {
            throw new IllegalArgumentException("Workflow cannot be null.");
        }
        logger.info("Loading new workflow.");
        this.workflowManager = this.workflowManagerFactory.createWorkflowManager(workflow);
    }

    /**
     * Ejects the currently loaded workflow, making the engine ready for a new workflow.
     */
    public void ejectWorkflow() {
        if (this.workflowManager == null) {
            throw new IllegalStateException("No workflow is currently loaded to eject.");
        }
        logger.info("Ejecting current workflow.");
        this.workflowManager = null;
    }

    /**
     * Executes the currently loaded workflow asynchronously.
     * @return A CompletableFuture that completes when the workflow finishes execution.
     */
    public CompletableFuture<Void> executeWorkflowAsync() {
        if (this.workflowManager == null) {
            throw new IllegalStateException("No workflow is loaded. Load a workflow before execution.");
        }

        cancelRequested = false;
        logger.info("Starting workflow execution asynchronously.");

        Workflow workflow = workflowManager.getWorkflow();

        return CompletableFuture.runAsync(() -> {
            if (workflow.getWorkflow().getExecutionStrategy() == Workflow.ExecutionStrategy.PARALLEL) {
                executeParallelWorkflow(workflow);
            } else {
                executeSequentialWorkflow(workflow);
            }
        }, executorService);
    }

    /**
     * Cancels the currently running workflow execution.
     */
    public void abortWorkflow() {
        if (this.workflowManager == null) {
            throw new IllegalStateException("No workflow is loaded to abort.");
        }

        logger.info("Aborting workflow execution.");
        cancelRequested = true;

        for (Map.Entry<BaseWorkflowNode, CompletableFuture<WorkflowOutputData>> entry : activeNodes.entrySet()) {
            logger.info("Aborting node: {}", entry.getKey());
            handleNodeAbort(entry.getKey(), entry.getValue());
        }
        activeNodes.clear();
    }

    /**
     * Executes a workflow sequentially.
     */
    private void executeSequentialWorkflow(Workflow workflow) {
        logger.info("Starting sequential workflow execution.");

        for (Workflow.NodeDefinition nodeDef : workflow.getWorkflow().getNodes()) {
            if (cancelRequested) {
                break;
            }

            BaseWorkflowNode node = nodeRegistry.getNode(nodeDef.getType());
            if (node == null) {
                throw new IllegalArgumentException("Unknown node type: " + nodeDef.getType());
            }

            logger.info("Executing node: {} of type: {}", nodeDef.getId(), nodeDef.getType());
            WorkflowInputData inputData = new WorkflowInputData(nodeDef.getConfig(), null);

            CompletableFuture<WorkflowOutputData> future = node.runAsync(nodeDef.getId(), inputData)
                .thenApply(workflowOutputData -> handleNodeCompletion(node, workflowOutputData));

            activeNodes.put(node, future);
            logger.info("Active node added.");

            future.join();
        }

        logger.info("Sequential workflow execution completed.");
    }

    /**
     * Executes a workflow in parallel asynchronously.
     */
    private void executeParallelWorkflow(Workflow workflow) {
        logger.info("Starting parallel workflow execution.");
        List<CompletableFuture<WorkflowOutputData>> futures = new ArrayList<>();

        for (Workflow.NodeDefinition nodeDef : workflow.getWorkflow().getNodes()) {
            if (cancelRequested) {
                break;
            }

            BaseWorkflowNode node = nodeRegistry.getNode(nodeDef.getType());
            if (node == null) {
                throw new IllegalArgumentException("Unknown node type: " + nodeDef.getType());
            }

            logger.info("Executing node: {} of type: {} in parallel", nodeDef.getId(), nodeDef.getType());

            CompletableFuture<WorkflowOutputData> future = node.runAsync(nodeDef.getId(), new WorkflowInputData(nodeDef.getConfig(), null))
                    .thenApply(workflowOutputData -> handleNodeCompletion(node, workflowOutputData));

            activeNodes.put(node, future);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        logger.info("Parallel workflow execution completed.");
    }

    /**
     * Handles node completion
     */
    private WorkflowOutputData handleNodeCompletion(BaseWorkflowNode node, WorkflowOutputData workflowOutputData) {
        if (workflowManager == null) {
            throw new IllegalStateException("No workflow is loaded. Cannot update workflow state.");
        }

        String nodeId = node.getNodeId();

        workflowManager.updateWorkflow(workflowOutputData);
        activeNodes.remove(node);
        return workflowOutputData;
    }

    /**
     * Handles node shutdown properly if workflow cancellation is requested.
     */
    private WorkflowOutputData handleNodeAbort(BaseWorkflowNode node, CompletableFuture<WorkflowOutputData> future) {
        if (workflowManager == null) {
            throw new IllegalStateException("No workflow is loaded. Cannot update workflow state.");
        }

        String nodeId = node.getNodeId();

        logger.info("Shutdown requested for node: {}", nodeId);
        node.abort();

        try {
            if (future != null) {
                WorkflowOutputData workflowOutputData = future.get(NODE_SHUTDOWN_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                workflowManager.updateWorkflow(workflowOutputData);
                activeNodes.remove(node); // Remove after execution
                return workflowOutputData;
            }
        } catch (TimeoutException e) {
            logger.warn("Node {} did not shut down within the allowed time ({} ms).", nodeId, NODE_SHUTDOWN_TIMEOUT_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Shutdown of node {} was interrupted.", nodeId);
        } catch (ExecutionException e) {
            logger.error("Error while shutting down node {}: {}", nodeId, e.getMessage());
        }

        activeNodes.remove(node); // Ensure node is removed even if shutdown fails
        WorkflowOutputData workflowOutputData = new WorkflowOutputData(WorkflowOutputData.Status.ABORTED,
                new com.fasterxml.jackson.databind.node.ObjectNode(com.fasterxml.jackson.databind.node.JsonNodeFactory.instance)
                        .put("message", "Node " + nodeId + " failed to shut down in time."));
        workflowManager.updateWorkflow(workflowOutputData);
        return workflowOutputData;
    }



}
