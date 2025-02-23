package com.example.workflow.parser;

import com.example.workflow.model.Workflow;
import com.example.workflow.nodes.NodeRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

/**
 * Parses workflow definitions from JSON files into Java objects.
 * Uses NodeRegistry for node validation.
 */
@Component
public class WorkflowParser {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowParser.class);
    private final ObjectMapper objectMapper;
    private final NodeRegistry nodeRegistry;

    /**
     * Constructor that injects NodeRegistry.
     * @param nodeRegistry The registry containing available workflow nodes.
     */
    @Autowired
    public WorkflowParser(NodeRegistry nodeRegistry) {
        this.objectMapper = new ObjectMapper();
        this.nodeRegistry = nodeRegistry;
    }

    /**
     * Parses a workflow definition from a JSON file.
     * @param file The JSON file containing the workflow definition.
     * @return A CompletableFuture containing the parsed Workflow object.
     */
    public CompletableFuture<Workflow> parseAsync(File file) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Parsing workflow definition from file: {}", file.getAbsolutePath());
            try {
                JsonNode jsonNode = objectMapper.readTree(file);
                return parseJsonNode(jsonNode);
            } catch (IOException e) {
                throw new CompletionException(
                        new IllegalArgumentException("Failed to read or parse workflow file: " + e.getMessage(), e)
                );
            }
        });
    }

    /**
     * Parses a workflow definition from a JSON string.
     * @param json The JSON string containing the workflow definition.
     * @return A CompletableFuture containing the parsed Workflow object.
     */
    public CompletableFuture<Workflow> parseAsync(String json) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Parsing workflow definition from string input.");
            try {
                JsonNode jsonNode = objectMapper.readTree(json);
                return parseJsonNode(jsonNode);
            } catch (IOException e) {
                throw new CompletionException(
                        new IllegalArgumentException("Failed to parse workflow from string: " + e.getMessage(), e)
                );
            }
        });
    }

    /**
     * Parses a workflow definition from a Jackson JsonNode object.
     * @param jsonNode The JsonNode containing the workflow definition.
     * @return A CompletableFuture containing the parsed Workflow object.
     */
    public CompletableFuture<Workflow> parseAsync(JsonNode jsonNode) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Parsing workflow definition from JsonNode.");
            return parseJsonNode(jsonNode);
        });
    }

    /**
     * Handles parsing and validation for all JSON sources.
     * @param jsonNode The JsonNode representing the workflow.
     * @return The validated Workflow object.
     */
    private Workflow parseJsonNode(JsonNode jsonNode) {
        try {
            Workflow workflow = objectMapper.treeToValue(jsonNode, Workflow.class);
            validateWorkflow(workflow);
            return workflow;
        } catch (JsonProcessingException e) {
            throw new CompletionException(
                    new IllegalArgumentException("Error mapping JSON to Workflow object: " + e.getMessage(), e)
            );
        }
    }

    /**
     * Validates the workflow structure, ensuring unique node IDs and valid types.
     * @param workflow The workflow to validate.
     */
    private void validateWorkflow(Workflow workflow) {
        Set<String> nodeIds = new HashSet<>();
        Set<String> duplicateIds = new HashSet<>();

        workflow.getWorkflow().getNodes().forEach(node -> {
            // Check for missing nodeId
            if (node.getId() == null || node.getId().trim().isEmpty()) {
                throw new CompletionException(
                        new IllegalArgumentException("Workflow contains a node with a missing nodeId.")
                );
            }

            // Check for duplicate nodeId
            if (!nodeIds.add(node.getId())) {
                duplicateIds.add(node.getId());
            }

            // Validate node type using NodeRegistry
            if (nodeRegistry.getNode(node.getType()) == null) {
                throw new CompletionException(
                        new IllegalArgumentException("Invalid node type: " + node.getType())
                );
            }
        });

        if (!duplicateIds.isEmpty()) {
            throw new CompletionException(
                    new IllegalArgumentException("Workflow contains duplicate nodeIds: " + duplicateIds.stream().collect(Collectors.joining(", ")))
            );
        }
    }

    /**
     * Serializes a Workflow object to a compact JSON string asynchronously.
     * @param workflow The Workflow object to serialize.
     * @return A CompletableFuture containing the JSON string.
     */
    public CompletableFuture<String> serializeAsync(Workflow workflow) {
        return serializeAsync(workflow, false); // Default to compact JSON
    }

    /**
     * Serializes a Workflow object to a JSON string asynchronously.
     * @param workflow The Workflow object to serialize.
     * @param formatted If true, the JSON will be formatted for readability.
     * @return A CompletableFuture containing the JSON string.
     */
    public CompletableFuture<String> serializeAsync(Workflow workflow, boolean formatted) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (formatted) {
                    return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(workflow);
                } else {
                    return objectMapper.writeValueAsString(workflow);
                }
            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize Workflow object to JSON", e);
                throw new CompletionException(new RuntimeException("Failed to serialize Workflow object to JSON", e));
            }
        });
    }
}
