package com.example.workflow.engine;

import com.example.workflow.model.Workflow;
import com.example.workflow.model.WorkflowOutputData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages updates to the Workflow instance in a thread-safe manner.
 */
public class WorkflowManager {

    private final Workflow workflow;
    private final Lock lock = new ReentrantLock();
    private final ObjectMapper objectMapper;

    public WorkflowManager(Workflow workflow, ObjectMapper objectMapper) {
        this.workflow = workflow;
        this.objectMapper = objectMapper;
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    public void updateWorkflow(WorkflowOutputData outputData) {
        lock.lock();
        try {
            // Convert outputData to a JsonNode
            JsonNode jsonOutput = objectMapper.valueToTree(outputData);
            // Remove the "nodeId" property if it exists
            if (jsonOutput.has("nodeId") && jsonOutput instanceof ObjectNode) {
                ((ObjectNode) jsonOutput).remove("nodeId");
            }
            // Update the node's output in the workflow
            workflow.getWorkflow().getNodes().stream()
                    .filter(node -> node.getId().equals(outputData.getNodeId()))
                    .findFirst()
                    .ifPresent(node -> node.setOutput(jsonOutput));
        } finally {
            lock.unlock();
        }
    }
}
