package io.orkes.samples.workers;

import com.netflix.conductor.sdk.workflow.task.WorkerTask;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import lombok.Data;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class SimpleWorker {

    @Data
    public static class SimpleWorkerInput {
        // Empty class as the original worker didn't use any inputs
        // But maintained for consistency and potential future use
    }

    @WorkerTask("simple_worker")
    @Tool(description = "A simple worker that returns the current time and a greeting")
    public Map<String, Object> executeSimpleTask(
            @ToolParam(description = "Input parameters") SimpleWorkerInput input) {

        Map<String, Object> result = new HashMap<>();

        // Generate current time
        String currentTimeOnServer = Instant.now().toString();

        // Add output data
        result.put("currentTimeOnServer", currentTimeOnServer);
        result.put("message", "Hello World!");

        // Include log in output data
        result.put("log", "This is a test log at time: " + currentTimeOnServer);

        return result;
    }
}