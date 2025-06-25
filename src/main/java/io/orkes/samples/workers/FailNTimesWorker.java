package io.orkes.samples.workers;

import com.netflix.conductor.sdk.workflow.task.WorkerTask;
import lombok.Data;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@Component
public class FailNTimesWorker {

    public static final String ZONE = "America/Los_Angeles";

    @Data
    public static class FailNTimesInput {
        private int timesToFail = 1;
        private int failedCount = 0;
        private int retryCount = 0;
        private long startTime;
        private long scheduledTime;
    }

    @WorkerTask("fail_n_times")
    @Tool(description = "A task that fails N times before succeeding, used for testing retry mechanisms")
    public Map<String, Object> failNTimes(
            @ToolParam(description = "Input parameters") FailNTimesInput input) {

        Map<String, Object> result = new HashMap<>();

        if(input.getRetryCount() == 0) {
            result.put("initialStartTime", Instant.ofEpochMilli(input.getStartTime()).atZone(ZoneId.of(ZONE)).toString());
            result.put("initialTaskExecutionTime", Instant.now().atZone(ZoneId.of(ZONE)).toString());
            result.put("initialScheduledTime", Instant.ofEpochMilli(input.getScheduledTime()).atZone(ZoneId.of(ZONE)).toString());
        }

        int failedCount = input.getFailedCount();

        if(failedCount >= input.getTimesToFail()) {
            result.put("outputVal", "This task completed failing for configured number of times - " + input.getTimesToFail());
            return result; // Returns with success status
        }

        result.put("failedCount", ++failedCount);
        result.put("outputVal", "This task fails (for testing) for " + input.getTimesToFail() + " times");

        // Throw exception to indicate failure
        throw new RuntimeException("Task failing as per configuration: " + failedCount + " of " + input.getTimesToFail() + " failures");
    }
}