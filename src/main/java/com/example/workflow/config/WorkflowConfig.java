package com.example.workflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class WorkflowConfig {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowConfig.class);
    private final ExecutorService executorService;

    // ✅ Default constructor for normal Spring use
    public WorkflowConfig() {
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    // ✅ Constructor for injecting mock ExecutorService (used in tests)
    public WorkflowConfig(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Bean
    public ExecutorService executorService() {
        return executorService;
    }

    @PreDestroy
    public void shutdownExecutorService() {
        logger.info("Initiating shutdown of ExecutorService...");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.warn("ExecutorService did not terminate in the allotted time. Forcing shutdown...");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.error("Interrupted during ExecutorService shutdown. Forcing shutdown now.", e);
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
