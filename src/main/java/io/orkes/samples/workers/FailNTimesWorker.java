package io.orkes.samples.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

@Component
public class FailNTimesWorker implements Worker {

    public static final String ZONE = "America/Los_Angeles";

    @Override
    public String getTaskDefName() {
        return "fail_n_times";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        if(task.getRetryCount() == 0) {
            result.addOutputData("initialStartTime", Instant.ofEpochMilli(task.getStartTime()).atZone(ZoneId.of(ZONE)).toString());
            result.addOutputData("initialTaskExecutionTime", Instant.now().atZone(ZoneId.of(ZONE)).toString());
            result.addOutputData("initialScheduledTime", Instant.ofEpochMilli(task.getScheduledTime()).atZone(ZoneId.of(ZONE)).toString());
        }
        int timesToFail = getIntValue("timesToFail", 1, task.getInputData());
        int failedCount = getIntValue("failedCount", 0, task.getOutputData());
        if(failedCount >= timesToFail) {
            result.addOutputData("outputVal", "This task completed failing for configured number of times - " + timesToFail);
            result.setStatus(TaskResult.Status.COMPLETED);
            return result;
        }
        result.addOutputData("failedCount", ++failedCount);
        result.addOutputData("outputVal", "This task fails (for testing) for " + timesToFail + " times");
        result.setStatus(TaskResult.Status.FAILED);
        return result;
    }

    private static int getIntValue(String paramName, int defaultValue, Map<String, Object> data) {
        return ((Number) (data.getOrDefault(paramName, defaultValue))).intValue();
    }
}
