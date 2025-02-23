package com.example.workflow.model;

import java.util.Map;

/**
 * Represents read-only input data passed to workflow nodes.
 */
public class WorkflowInputData {

    private final Map<String, Object> config;
    private final Map<String, Object> context;

    public WorkflowInputData(Map<String, Object> config, Map<String, Object> context) {
        this.config = config;
        this.context = context;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public Map<String, Object> getContext() {
        return context;
    }
}
