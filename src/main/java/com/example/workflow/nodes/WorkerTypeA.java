package com.example.workflow.nodes;

import com.example.workflow.model.WorkflowInputData;
import com.example.workflow.model.WorkflowOutputData;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class WorkerTypeA extends BaseWorkflowNode {

    private static final Logger logger = LoggerFactory.getLogger(WorkerTypeA.class);
    private volatile boolean shutdownRequested = false;

    @Override
    protected WorkflowOutputData execute(WorkflowInputData input) {
        logger.info("WorkerTypeA started with config: {}", input.getConfig());
        try {
            for (int i = 0; i < 10; i++) {
                if (shutdownRequested) {
                    logger.info("Shutdown requested. Exiting WorkerTypeA execution early.");
                    return new WorkflowOutputData(WorkflowOutputData.Status.FAIL,
                            JsonNodeFactory.instance.objectNode().put("message", "Aborted by shutdown"));
                }
                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new WorkflowOutputData(WorkflowOutputData.Status.FAIL,
                    JsonNodeFactory.instance.objectNode().put("message", "Interrupted"));
        }
        logger.info("WorkerTypeA completed successfully.");
        return new WorkflowOutputData(WorkflowOutputData.Status.PASS,
                JsonNodeFactory.instance.objectNode().put("result", "Data processed by WorkerTypeA"));
    }

    @Override
    public void abort() {
        logger.info("Abort called on WorkerTypeA.");
        shutdownRequested = true;
    }
}
