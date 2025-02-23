package com.example.workflow.engine;

import com.example.workflow.model.Workflow;
import com.example.workflow.model.Workflow.WorkflowDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowManagerFactoryTest {

    private WorkflowManagerFactory factory;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Use a default ObjectMapper instance for testing.
        objectMapper = new ObjectMapper();
        factory = new WorkflowManagerFactory(objectMapper);
    }

    @Test
    void testCreateWorkflowManager_WithValidWorkflow() {
        // Create a sample workflow instance with details.
        Workflow workflow = new Workflow();
        WorkflowDetails details = new WorkflowDetails();
        workflow.setWorkflow(details);

        // Create the manager
        WorkflowManager manager = factory.createWorkflowManager(workflow);

        // Verify the manager is not null and holds the correct workflow.
        assertNotNull(manager, "WorkflowManager should not be null.");
        assertEquals(workflow, manager.getWorkflow(), "The workflow returned by the manager should match the input workflow.");
    }

    @Test
    void testCreateWorkflowManager_WithNullWorkflow() {
        // Create a manager with a null workflow.
        WorkflowManager manager = factory.createWorkflowManager(null);

        // Verify the manager is not null and the workflow is null.
        assertNotNull(manager, "WorkflowManager should be created even with null workflow.");
        assertNull(manager.getWorkflow(), "The workflow in the manager should be null when provided null.");
    }

    @Test
    void testMultipleInstances_AreDistinct() {
        // Create a sample workflow instance.
        Workflow workflow = new Workflow();
        WorkflowDetails details = new WorkflowDetails();
        workflow.setWorkflow(details);

        // Create two manager instances
        WorkflowManager manager1 = factory.createWorkflowManager(workflow);
        WorkflowManager manager2 = factory.createWorkflowManager(workflow);

        // Verify that each call to createWorkflowManager produces a new instance.
        assertNotSame(manager1, manager2, "Each call to createWorkflowManager should produce a distinct instance.");
    }
}
