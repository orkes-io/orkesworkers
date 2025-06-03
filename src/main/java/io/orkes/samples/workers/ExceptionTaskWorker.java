package io.orkes.samples.workers;

import com.netflix.conductor.sdk.workflow.task.WorkerTask;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ExceptionTaskWorker {

    @Data
    public static class ExceptionTaskInput {
        private int retryCount;
    }

    @WorkerTask("always_throws_exception_worker")
    @Tool(description = "A task that always throws an exception (for testing failure scenarios)")
    public void alwaysThrowsException(
            @ToolParam(description = "Input parameters") ExceptionTaskInput input) {

        log.info("Retry count: {}", input.getRetryCount());
        throw new RuntimeException("I'm a task that always throws exceptions");
    }
}