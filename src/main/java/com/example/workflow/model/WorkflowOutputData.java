package com.example.workflow.model;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents the output data returned by workflow nodes.
 */
public class WorkflowOutputData {

    private String nodeId;
    private final Status status;
    private final JsonNode output;

    public WorkflowOutputData(Status status, JsonNode output) {
        this.status = status;
        this.output = output;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public Status getStatus() {
        return status;
    }

    public JsonNode getOutput() {
        return output;
    }

    /**
     * Enum representing execution status of a node.
     */
    public enum Status {
        PASS,
        FAIL,
        ABORTED
    }
}
