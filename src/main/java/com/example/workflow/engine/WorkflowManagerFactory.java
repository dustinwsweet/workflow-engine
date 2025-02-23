package com.example.workflow.engine;

import com.example.workflow.model.Workflow;
import org.springframework.stereotype.Component;

/**
 * Factory for creating WorkflowManager instances on demand.
 */
@Component
public class WorkflowManagerFactory {

    /**
     * Creates a new instance of WorkflowManager for a given workflow.
     * @param workflow The workflow instance.
     * @return A new WorkflowManager instance.
     */
    public WorkflowManager createWorkflowManager(Workflow workflow) {
        return new WorkflowManager(workflow);
    }
}
