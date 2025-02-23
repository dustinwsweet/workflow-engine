package com.example.workflow.engine;

import com.example.workflow.model.Workflow;
import com.example.workflow.model.WorkflowOutputData;
import com.fasterxml.jackson.databind.ObjectMapper;

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
            workflow.getWorkflow().getNodes().stream()
                    .filter(node -> node.getId().equals(outputData.getNodeId()))
                    .findFirst()
                    .ifPresent(node -> node.setOutput(objectMapper.valueToTree(outputData)));
        } finally {
            lock.unlock();
        }
    }
}
