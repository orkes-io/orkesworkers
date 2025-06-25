package io.orkes.samples.workers;

import com.netflix.conductor.sdk.workflow.task.WorkerTask;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class TaskCallbackWorker {

    @Data
    public static class TaskCallbackInput {
        // Empty class as the original worker didn't use any inputs
        // But maintained for consistency and potential future use
    }

    private AtomicInteger runCount = new AtomicInteger(0);

    @WorkerTask("test_task_callback")
    @Tool(description = "A task that demonstrates callback functionality by incrementing a counter and completing after 5 runs")
    public Map<String, Object> executeTaskCallback(
            @ToolParam(description = "Input parameters") TaskCallbackInput input) {

        Map<String, Object> result = new HashMap<>();

        // Increment run count and add to output
        int currentCount = runCount.incrementAndGet();
        result.put("result" + currentCount, "Run Count: " + currentCount);

        // Add log message to output
        result.put("log", "Running for " + currentCount);

        // Check if we should complete the task
        if (currentCount % 5 == 0) {

        } else {
            result.put("taskStatus", "IN_PROGRESS");
            result.put("callbackAfterSeconds", 3);
        }

        return result;
    }
}