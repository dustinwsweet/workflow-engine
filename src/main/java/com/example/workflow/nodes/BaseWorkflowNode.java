package com.example.workflow.nodes;

import com.example.workflow.model.WorkflowInputData;
import com.example.workflow.model.WorkflowOutputData;
import java.util.concurrent.CompletableFuture;

/**
 * Base class for all workflow nodes, handling common functionality.
 */
public abstract class BaseWorkflowNode {

    protected String nodeId;

    /**
     * Launches the node execution asynchronously.
     */
    public final CompletableFuture<WorkflowOutputData> runAsync(String nodeId, WorkflowInputData input) {

        if (nodeId == null) {
            throw new IllegalArgumentException("nodeId property cannot be null.");
        }
        this.nodeId = nodeId;

        return CompletableFuture.supplyAsync(() -> {
            WorkflowOutputData outputData = execute(input);
            if (outputData != null) {
                outputData.setNodeId(nodeId);
            }
            return outputData;
        });
    }

    public String getNodeId() {
        return this.nodeId;
    }

    /**
     * Abstract method that must be implemented by subclasses to define node execution logic.
     */
    protected abstract WorkflowOutputData execute(WorkflowInputData input);

    /**
     * Abstract method to abort the node execution.
     */
    public abstract void abort();
}
