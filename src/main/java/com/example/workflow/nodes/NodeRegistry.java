package com.example.workflow.nodes;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Manages the registry of workflow nodes, allowing dynamic discovery and retrieval.
 */
@Component
public class NodeRegistry {

    private static final Logger logger = LoggerFactory.getLogger(NodeRegistry.class);
    private final Map<String, BaseWorkflowNode> nodeInstances = new HashMap<>();

    /**
     * Initializes the registry by discovering and instantiating available workflow node types.
     */
    public NodeRegistry() {
        discoverValidNodeTypes();
    }

    /**
     * Scans the package for subclasses of BaseWorkflowNode and registers them.
     */
    private void discoverValidNodeTypes() {
        Reflections reflections = new Reflections("com.example.workflow.nodes");
        Set<Class<? extends BaseWorkflowNode>> nodeClasses = reflections.getSubTypesOf(BaseWorkflowNode.class);

        for (Class<? extends BaseWorkflowNode> nodeClass : nodeClasses) {
            try {
                BaseWorkflowNode nodeInstance = nodeClass.getDeclaredConstructor().newInstance();
                nodeInstances.put(nodeClass.getSimpleName(), nodeInstance);
                logger.info("Registered workflow node: {}", nodeClass.getSimpleName());
            } catch (Exception e) {
                logger.error("Failed to instantiate node class: {}", nodeClass.getName(), e);
            }
        }
    }

    /**
     * Retrieves a registered node by type.
     * @param type The name of the node type.
     * @return The node instance, or null if not found.
     */
    public BaseWorkflowNode getNode(String type) {
        return nodeInstances.get(type);
    }

    /**
     * Returns the full registry of available nodes.
     * @return Map of node types to node instances.
     */
    public Map<String, BaseWorkflowNode> getAllNodes() {
        return nodeInstances;
    }
}
