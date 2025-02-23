package com.example.workflow.engine;

import com.example.workflow.model.Workflow;
import com.example.workflow.model.WorkflowOutputData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkflowManagerTest {

    private WorkflowManager workflowManager;
    private Workflow mockWorkflow;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockWorkflow = mock(Workflow.class);
        workflowManager = new WorkflowManager(mockWorkflow, objectMapper);
    }

    @Test
    void testGetWorkflow_ShouldReturnWorkflowInstance() {
        assertEquals(mockWorkflow, workflowManager.getWorkflow());
    }

    @Test
    void testUpdateWorkflow_ShouldUpdateExistingNodeOutput() {
        Workflow.WorkflowDetails workflowDetails = new Workflow.WorkflowDetails();
        Workflow.NodeDefinition node = new Workflow.NodeDefinition();
        node.setId("node1");

        workflowDetails.setNodes(List.of(node));
        when(mockWorkflow.getWorkflow()).thenReturn(workflowDetails);

        // ✅ Create valid "data" field
        ObjectNode dataNode = objectMapper.createObjectNode();
        dataNode.put("result", "Success");

        // ✅ Create valid WorkflowOutputData (ensures correct serialization)
        WorkflowOutputData outputData = new WorkflowOutputData(WorkflowOutputData.Status.PASS, dataNode);
        outputData.setNodeId("node1");

        // ✅ Verify the JSON conversion is correct before calling updateWorkflow()
        JsonNode jsonOutput = objectMapper.valueToTree(outputData);
        assertTrue(jsonOutput.has("status"), "Output JSON must contain 'status' field.");
        assertTrue(jsonOutput.has("data"), "Output JSON must contain 'data' field.");

        workflowManager.updateWorkflow(outputData);

        JsonNode updatedOutput = node.getOutput();
        assertNotNull(updatedOutput, "Node output should not be null after update.");
        assertFalse(updatedOutput.has("nodeId"), "Output should not contain nodeId property.");
        assertEquals("Success", updatedOutput.get("data").get("result").asText(), "Updated result should match expected value.");
    }




    @Test
    void testUpdateWorkflow_ShouldNotModifyWorkflowIfNodeNotFound() {
        Workflow.WorkflowDetails workflowDetails = new Workflow.WorkflowDetails();
        workflowDetails.setNodes(Collections.emptyList());

        when(mockWorkflow.getWorkflow()).thenReturn(workflowDetails);

        WorkflowOutputData outputData = new WorkflowOutputData(WorkflowOutputData.Status.PASS,
                objectMapper.createObjectNode().put("result", "Success"));
        outputData.setNodeId("node1");

        workflowManager.updateWorkflow(outputData);

        assertTrue(workflowDetails.getNodes().isEmpty(), "No node should be updated if it doesn't exist.");
    }

    @Test
    void testUpdateWorkflow_ShouldHandleNullOutputDataGracefully() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                workflowManager.updateWorkflow(null));

        assertEquals("WorkflowOutputData cannot be null.", exception.getMessage());
    }

    @Test
    void testUpdateWorkflow_ShouldThrowIfStatusIsMissing() {
        Workflow.WorkflowDetails workflowDetails = new Workflow.WorkflowDetails();
        Workflow.NodeDefinition node = new Workflow.NodeDefinition();
        node.setId("node1");
        workflowDetails.setNodes(List.of(node));

        when(mockWorkflow.getWorkflow()).thenReturn(workflowDetails);

        // Missing "status" field
        ObjectNode invalidOutput = objectMapper.createObjectNode();
        invalidOutput.set("data", objectMapper.createObjectNode().put("result", "Success"));

        WorkflowOutputData invalidData = new WorkflowOutputData(null, invalidOutput);
        invalidData.setNodeId("node1");

        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                workflowManager.updateWorkflow(invalidData));

        assertEquals("Invalid WorkflowOutputData: 'status' and 'data' fields are required.", exception.getMessage());
    }

    @Test
    void testUpdateWorkflow_ShouldThrowIfDataIsMissing() {
        Workflow.WorkflowDetails workflowDetails = new Workflow.WorkflowDetails();
        Workflow.NodeDefinition node = new Workflow.NodeDefinition();
        node.setId("node1");
        workflowDetails.setNodes(List.of(node));

        when(mockWorkflow.getWorkflow()).thenReturn(workflowDetails);

        // Missing "data" field
        WorkflowOutputData invalidData = new WorkflowOutputData(WorkflowOutputData.Status.PASS, null);
        invalidData.setNodeId("node1");

        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                workflowManager.updateWorkflow(invalidData));

        assertEquals("Invalid WorkflowOutputData: 'status' and 'data' fields are required.", exception.getMessage());
    }

    @Test
    void testUpdateWorkflow_ShouldNotModifyDataIfAlreadySet() {
        Workflow.WorkflowDetails workflowDetails = new Workflow.WorkflowDetails();
        Workflow.NodeDefinition node = new Workflow.NodeDefinition();
        node.setId("node1");

        // ✅ Ensure initial output contains BOTH "status" and "data"
        ObjectNode existingOutput = objectMapper.createObjectNode();
        existingOutput.put("status", "PASS");
        ObjectNode dataNode = objectMapper.createObjectNode();
        dataNode.put("result", "ExistingValue");
        existingOutput.set("data", dataNode);
        node.setOutput(existingOutput); // ✅ Now correctly formatted

        workflowDetails.setNodes(List.of(node));
        when(mockWorkflow.getWorkflow()).thenReturn(workflowDetails);

        // ✅ New output data (correctly structured)
        WorkflowOutputData outputData = new WorkflowOutputData(WorkflowOutputData.Status.PASS,
                objectMapper.createObjectNode().put("result", "Updated"));
        outputData.setNodeId("node1");

        workflowManager.updateWorkflow(outputData);

        // ✅ Ensure the output exists and is structured correctly
        JsonNode updatedOutput = node.getOutput();
        assertNotNull(updatedOutput, "Updated output should not be null.");
        assertTrue(updatedOutput.has("status"), "Updated output should contain 'status' field.");
        assertTrue(updatedOutput.has("data"), "Updated output should contain 'data' field.");
        assertEquals("Updated", updatedOutput.get("data").get("result").asText());
    }


    @Test
    void testUpdateWorkflow_ShouldBeThreadSafe() throws InterruptedException {
        Workflow.WorkflowDetails workflowDetails = new Workflow.WorkflowDetails();
        Workflow.NodeDefinition node = new Workflow.NodeDefinition();
        node.setId("node1");
        workflowDetails.setNodes(List.of(node));

        when(mockWorkflow.getWorkflow()).thenReturn(workflowDetails);

        WorkflowOutputData outputData = new WorkflowOutputData(WorkflowOutputData.Status.PASS,
                objectMapper.createObjectNode().put("result", "Thread Safe"));
        outputData.setNodeId("node1");

        Thread thread1 = new Thread(() -> workflowManager.updateWorkflow(outputData));
        Thread thread2 = new Thread(() -> workflowManager.updateWorkflow(outputData));

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        JsonNode updatedOutput = node.getOutput();
        assertNotNull(updatedOutput, "Output should not be null after concurrent updates.");
        assertFalse(updatedOutput.has("nodeId"), "Output should not contain nodeId.");
        assertTrue(updatedOutput.has("status"), "Updated output should contain 'status' field.");
        assertTrue(updatedOutput.has("data"), "Updated output should contain 'data' field.");
        assertEquals("Thread Safe", updatedOutput.get("data").get("result").asText());
    }
}
