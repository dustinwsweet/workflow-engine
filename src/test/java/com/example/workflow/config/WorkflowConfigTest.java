package com.example.workflow.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowConfigTest {

    private WorkflowConfig workflowConfig;

    @Mock
    private ExecutorService executorServiceMock;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // ✅ Inject the mock ExecutorService
        workflowConfig = new WorkflowConfig(executorServiceMock);
    }

    @Test
    void testExecutorServiceBean_ShouldReturnValidExecutorService() {
        ExecutorService executorService = workflowConfig.executorService();
        assertNotNull(executorService, "ExecutorService should not be null");
    }

    @Test
    void testShutdownExecutorService_ShouldShutdownExecutorGracefully() throws InterruptedException {
        doNothing().when(executorServiceMock).shutdown();
        when(executorServiceMock.awaitTermination(10, TimeUnit.SECONDS)).thenReturn(true);

        workflowConfig.shutdownExecutorService();

        verify(executorServiceMock, times(1)).shutdown();
        verify(executorServiceMock, times(1)).awaitTermination(10, TimeUnit.SECONDS);
        verify(executorServiceMock, never()).shutdownNow();
    }

    @Test
    void testShutdownExecutorService_WhenTerminationTimesOut_ShouldForceShutdown() throws InterruptedException {
        doNothing().when(executorServiceMock).shutdown();
        when(executorServiceMock.awaitTermination(10, TimeUnit.SECONDS)).thenReturn(false);

        workflowConfig.shutdownExecutorService();

        verify(executorServiceMock, times(1)).shutdown();
        verify(executorServiceMock, times(1)).awaitTermination(10, TimeUnit.SECONDS);
        verify(executorServiceMock, times(1)).shutdownNow();
    }

    @Test
    void testShutdownExecutorService_WhenInterruptedExceptionOccurs_ShouldForceShutdownAndInterruptThread() throws InterruptedException {
        doNothing().when(executorServiceMock).shutdown();
        doThrow(new InterruptedException("Test Interruption")).when(executorServiceMock).awaitTermination(10, TimeUnit.SECONDS);

        Thread currentThread = Thread.currentThread();
        boolean wasInterruptedBefore = currentThread.isInterrupted();

        workflowConfig.shutdownExecutorService();

        verify(executorServiceMock, times(1)).shutdown();
        verify(executorServiceMock, times(1)).shutdownNow();
        assertTrue(currentThread.isInterrupted(), "Current thread should be interrupted after InterruptedException");
    }
}
