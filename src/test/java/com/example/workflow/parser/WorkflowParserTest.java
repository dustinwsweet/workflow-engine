package com.example.workflow.parser;

import com.example.workflow.model.Workflow;
import com.example.workflow.nodes.NodeRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowParserTest {

    private NodeRegistry nodeRegistry;
    private WorkflowParser parser;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        nodeRegistry = new NodeRegistry();
        parser = new WorkflowParser(nodeRegistry);
        objectMapper = new ObjectMapper();
    }

    private String readJsonFileAsString(String filePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }

    private JsonNode readJsonFileAsJsonNode(String filePath) throws IOException {
        return objectMapper.readTree(new File(filePath));
    }

    @Test
    void testValidWorkflowParsing_File() throws IOException {
        File validFile = new File("src/test/resources/valid_workflow.json");
        Workflow workflow = parser.parseAsync(validFile).join();

        assertNotNull(workflow, "Workflow should not be null");
        assertNotNull(workflow.getWorkflow(), "Workflow details should not be null");
        assertFalse(workflow.getWorkflow().getNodes().isEmpty(), "Workflow should contain nodes");
    }

    @Test
    void testValidWorkflowParsing_String() throws IOException {
        String json = readJsonFileAsString("src/test/resources/valid_workflow.json");
        Workflow workflow = parser.parseAsync(json).join();

        assertNotNull(workflow, "Workflow should not be null");
        assertNotNull(workflow.getWorkflow(), "Workflow details should not be null");
        assertFalse(workflow.getWorkflow().getNodes().isEmpty(), "Workflow should contain nodes");
    }

    @Test
    void testValidWorkflowParsing_JsonNode() throws IOException {
        JsonNode jsonNode = readJsonFileAsJsonNode("src/test/resources/valid_workflow.json");
        Workflow workflow = parser.parseAsync(jsonNode).join();

        assertNotNull(workflow, "Workflow should not be null");
        assertNotNull(workflow.getWorkflow(), "Workflow details should not be null");
        assertFalse(workflow.getWorkflow().getNodes().isEmpty(), "Workflow should contain nodes");
    }

    private void assertAsyncException(Class<? extends Throwable> expected, CompletableFuture<?> future, String expectedMessage) {
        ExecutionException thrown = assertThrows(ExecutionException.class, future::get, "Expected an ExecutionException");

        assertInstanceOf(expected, thrown.getCause(), "Expected cause to be " + expected.getSimpleName());

        String actualMessage = thrown.getCause().getMessage();
        assertTrue(actualMessage.contains(expectedMessage),
                "Expected message to contain: \"" + expectedMessage + "\", but got: \"" + actualMessage + "\"");
    }


    @Test
    void testMissingNodeIdThrowsException_File() {
        File invalidFile = new File("src/test/resources/missing_node_id.json");
        assertAsyncException(IllegalArgumentException.class, parser.parseAsync(invalidFile), "missing nodeId");
    }

    @Test
    void testMissingNodeIdThrowsException_String() throws IOException {
        String json = readJsonFileAsString("src/test/resources/missing_node_id.json");
        assertAsyncException(IllegalArgumentException.class, parser.parseAsync(json), "missing nodeId");
    }

    @Test
    void testMissingNodeIdThrowsException_JsonNode() throws IOException {
        JsonNode jsonNode = readJsonFileAsJsonNode("src/test/resources/missing_node_id.json");
        assertAsyncException(IllegalArgumentException.class, parser.parseAsync(jsonNode), "missing nodeId");
    }

    @Test
    void testDuplicateNodeIdThrowsException_File() {
        File invalidFile = new File("src/test/resources/duplicate_node_id.json");
        assertAsyncException(IllegalArgumentException.class, parser.parseAsync(invalidFile), "duplicate nodeIds");
    }

    @Test
    void testDuplicateNodeIdThrowsException_String() throws IOException {
        String json = readJsonFileAsString("src/test/resources/duplicate_node_id.json");
        assertAsyncException(IllegalArgumentException.class, parser.parseAsync(json), "duplicate nodeIds");
    }

    @Test
    void testDuplicateNodeIdThrowsException_JsonNode() throws IOException {
        JsonNode jsonNode = readJsonFileAsJsonNode("src/test/resources/duplicate_node_id.json");
        assertAsyncException(IllegalArgumentException.class, parser.parseAsync(jsonNode), "duplicate nodeIds");
    }

    @Test
    void testInvalidNodeTypeThrowsException_File() {
        File invalidFile = new File("src/test/resources/invalid_node_type.json");
        assertAsyncException(IllegalArgumentException.class, parser.parseAsync(invalidFile), "Invalid node type");
    }

    @Test
    void testInvalidNodeTypeThrowsException_String() throws IOException {
        String json = readJsonFileAsString("src/test/resources/invalid_node_type.json");
        assertAsyncException(IllegalArgumentException.class, parser.parseAsync(json), "Invalid node type");
    }

    @Test
    void testInvalidNodeTypeThrowsException_JsonNode() throws IOException {
        JsonNode jsonNode = readJsonFileAsJsonNode("src/test/resources/invalid_node_type.json");
        assertAsyncException(IllegalArgumentException.class, parser.parseAsync(jsonNode), "Invalid node type");
    }

    @Test
    void testInvalidJsonFormatThrowsException_File() {
        File invalidFile = new File("src/test/resources/invalid_json_format.json");
        assertAsyncException(IllegalArgumentException.class, parser.parseAsync(invalidFile), "Failed to read or parse workflow file");
    }

    @Test
    void testInvalidJsonFormatThrowsException_String() throws IOException {
        String json = readJsonFileAsString("src/test/resources/invalid_json_format.json");
        assertAsyncException(IllegalArgumentException.class, parser.parseAsync(json), "Failed to parse workflow from string");
    }

    @Test
    void testInvalidJsonFormatThrowsException_JsonNode() throws IOException {
        JsonNode jsonNode;
        try {
            jsonNode = objectMapper.readTree(readJsonFileAsString("src/test/resources/invalid_json_format.json"));
        } catch (JsonProcessingException e) {
            // Create an invalid JsonNode manually to simulate broken JSON
            ObjectNode invalidNode = JsonNodeFactory.instance.objectNode();
            invalidNode.put("invalid_field", "{ this is broken json }"); // Corrupt JSON structure
            jsonNode = invalidNode;
        }

        assertAsyncException(IllegalArgumentException.class, parser.parseAsync(jsonNode), "Error mapping JSON to Workflow object");
    }

}
