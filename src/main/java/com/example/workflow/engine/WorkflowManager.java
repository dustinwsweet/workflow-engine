package com.example.workflow.engine;

import com.example.workflow.model.Workflow;
import com.example.workflow.model.WorkflowOutputData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages updates to the Workflow instance in a thread-safe manner.
 */
@Component
public class WorkflowManager {

    private final Workflow workflow;
    private final Lock lock = new ReentrantLock();

    @Autowired
    private final ObjectMapper objectMapper;

    /**
     * Constructor accepting a Workflow.
     * @param workflow Represents the structure of a workflow, including execution strategy and node list.
     */
    @Autowired
    public WorkflowManager(Workflow workflow) {
        this.workflow = workflow;
        this.objectMapper = new ObjectMapper();
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    /**
     * Updates the workflow with the output data of a completed node.
     *
     * @param outputData The output data from the node execution.
     */
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
