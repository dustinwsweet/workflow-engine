package com.example.workflow.model;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents the structure of a workflow, including execution strategy and node list.
 */
public class Workflow {

    private Metadata metadata;
    private WorkflowDetails workflow;

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public WorkflowDetails getWorkflow() {
        return workflow;
    }

    public void setWorkflow(WorkflowDetails workflow) {
        this.workflow = workflow;
    }

    /**
     * Enum representing execution strategies for workflow nodes.
     */
    public enum ExecutionStrategy {
        SEQUENTIAL,
        PARALLEL
    }

    /**
     * Nested class representing metadata for the workflow.
     */
    public static class Metadata {
        private String version;
        private String author;
        private String timestamp;

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }
    }

    /**
     * Nested class representing the workflow details.
     */
    public static class WorkflowDetails {
        private ExecutionStrategy executionStrategy;
        private List<NodeDefinition> nodes;

        public ExecutionStrategy getExecutionStrategy() {
            return executionStrategy;
        }

        public void setExecutionStrategy(ExecutionStrategy executionStrategy) {
            this.executionStrategy = executionStrategy;
        }

        public List<NodeDefinition> getNodes() {
            return nodes;
        }

        public void setNodes(List<NodeDefinition> nodes) {
            this.nodes = nodes;
        }
    }

    /**
     * Nested class representing a node definition in the workflow.
     */
    public static class NodeDefinition {
        private String id;
        private String type;
        private Map<String, Object> config;
        private JsonNode output; // Added output property

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Map<String, Object> getConfig() {
            return config;
        }

        public void setConfig(Map<String, Object> config) {
            this.config = config;
        }

        public JsonNode getOutput() {
            return output;
        }

        public void setOutput(JsonNode output) {
            this.output = output;
        }
    }
}
