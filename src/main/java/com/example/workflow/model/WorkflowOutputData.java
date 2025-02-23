package com.example.workflow.model;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents the output data returned by workflow nodes.
 */
public class WorkflowOutputData {

    private String nodeId;
    private final Status status;
    private final JsonNode data;

    public WorkflowOutputData(Status status, JsonNode output) {
        this.status = status;
        this.data = output;
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

    public JsonNode getData() {
        return data;
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
