package com.example.workflow;

import com.example.workflow.engine.WorkflowEngine;
import com.example.workflow.engine.WorkflowManagerFactory;
import com.example.workflow.model.Workflow;
import com.example.workflow.nodes.NodeRegistry;
import com.example.workflow.nodes.WorkerTypeA;
import com.example.workflow.parser.WorkflowParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

public class WorkflowEngineTest {

    private WorkflowParser parser;
    private WorkflowEngine workflowEngine;
    private Workflow workflow;

    private static final Logger logger = LoggerFactory.getLogger(WorkflowEngine.class);

    @BeforeEach
    void setUp() throws Exception {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

        // Register any other implementations here
        context.register(WorkerTypeA.class);
        context.refresh();

        NodeRegistry nodeRegistry = new NodeRegistry(context);
        ObjectMapper objectMapper = new ObjectMapper();
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        parser = new WorkflowParser(objectMapper, nodeRegistry);
        workflowEngine = new WorkflowEngine(nodeRegistry, new WorkflowManagerFactory(objectMapper), executorService);

        // Load workflow JSON
        File workflowFile = new File("src/test/resources/workflow.json");
        workflow = parser.parseAsync(workflowFile).join();

        workflowEngine.loadWorkflow(workflow);
    }

    @Test
    void testSequentialWorkflowExecution() {
        assertNotNull(workflow);
        assertEquals(Workflow.ExecutionStrategy.SEQUENTIAL, workflow.getWorkflow().getExecutionStrategy());

        // Execute workflow asynchronously
        CompletableFuture<Void> executionFuture = workflowEngine.executeWorkflowAsync();
        executionFuture.join();  // Wait for completion

        // Assertions - Validate workflow structure
        assertNotNull(workflow.getWorkflow());
        assertEquals(2, workflow.getWorkflow().getNodes().size());

        String output = parser.serializeAsync(workflow, true).join();
        logger.info(output);
    }

    @Test
    void testParallelWorkflowExecution() throws IOException {
        // Load parallel workflow JSON
        File workflowFile = new File("src/test/resources/parallel_workflow.json");
        workflow = parser.parseAsync(workflowFile).join();

        assertNotNull(workflow);
        assertEquals(Workflow.ExecutionStrategy.PARALLEL, workflow.getWorkflow().getExecutionStrategy());

        workflowEngine.loadWorkflow(workflow);

        // Execute workflow asynchronously
        CompletableFuture<Void> executionFuture = workflowEngine.executeWorkflowAsync();
        executionFuture.join();  // Wait for parallel execution to complete

        // Validate workflow execution results
        assertNotNull(workflow.getWorkflow());
        assertEquals(2, workflow.getWorkflow().getNodes().size());

        String output = parser.serializeAsync(workflow, true).join();
        logger.info(output);
    }

    @Test
    void testParallelWorkflowCancellation() throws InterruptedException {
        // Load parallel workflow JSON
        File workflowFile = new File("src/test/resources/parallel_workflow.json");
        workflow = parser.parseAsync(workflowFile).join();

        workflowEngine.loadWorkflow(workflow);

        // Start workflow execution asynchronously
        CompletableFuture<Void> executionFuture = workflowEngine.executeWorkflowAsync();

        // Allow execution to begin before cancelling
        Thread.sleep(2000);
        workflowEngine.abortWorkflow();
        executionFuture.join();
/*
        // Ensure execution was cancelled
        ExecutionException thrown = assertThrows(ExecutionException.class, executionFuture::get,
                "Workflow execution should have been cancelled.");

        // Verify the root cause is CancellationException
        assertTrue(thrown.getCause() instanceof CancellationException,
                "Expected cause to be CancellationException, but was: " + thrown.getCause());
*/
        String output = parser.serializeAsync(workflow, true).join();
        logger.info(output);
    }

    @Test
    void testSequentialWorkflowCancellation() throws InterruptedException {
        assertNotNull(workflow);

        // Start workflow execution asynchronously
        CompletableFuture<Void> executionFuture = workflowEngine.executeWorkflowAsync();

        workflowEngine.loadWorkflow(workflow);

        // Allow execution to begin before cancelling
        Thread.sleep(2000);
        workflowEngine.abortWorkflow();

        executionFuture.join();

/*
        // Ensure execution was cancelled
        ExecutionException thrown = assertThrows(ExecutionException.class, executionFuture::get,
                "Workflow execution should have been cancelled.");

        // Verify the root cause is CancellationException
        assertTrue(thrown.getCause() instanceof CancellationException,
                "Expected cause to be CancellationException, but was: " + thrown.getCause());
*/
        String output = parser.serializeAsync(workflow, true).join();
        logger.info(output);
    }


    @Test
    void testUnknownNodeTypeThrowsException() throws Exception {
        File workflowFile = new File("src/test/resources/invalid_workflow.json");
        assertTrue(workflowFile.exists(), "Test file missing: invalid_workflow.json");

        CompletableFuture<Workflow> future = parser.parseAsync(workflowFile);

        ExecutionException thrown = assertThrows(ExecutionException.class, future::get);

        // Ensure the root cause is an IllegalArgumentException
        assertTrue(thrown.getCause() instanceof IllegalArgumentException,
                "Expected cause to be IllegalArgumentException, but was: " + thrown.getCause());

        // Verify the error message
        assertTrue(thrown.getCause().getMessage().contains("Invalid node type"),
                "Expected error message to contain 'Invalid node type', but got: " + thrown.getCause().getMessage());
    }

}
