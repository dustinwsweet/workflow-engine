package com.example.workflow.nodes;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class NodeRegistry {

    private final Map<String, BaseWorkflowNode> nodeInstances = new HashMap<>();

    // Let Spring inject the context
    public NodeRegistry(ApplicationContext context) {
        // Retrieve all beans of type BaseWorkflowNode registered in the context.
        Map<String, BaseWorkflowNode> beans = context.getBeansOfType(BaseWorkflowNode.class);
        beans.values().forEach(node -> {
            // Register by simple class name or use a custom property/method to define the type key.
            nodeInstances.put(node.getClass().getSimpleName(), node);
        });
    }

    public BaseWorkflowNode getNode(String type) {
        return nodeInstances.get(type);
    }

    public Map<String, BaseWorkflowNode> getAllNodes() {
        return nodeInstances;
    }
}
