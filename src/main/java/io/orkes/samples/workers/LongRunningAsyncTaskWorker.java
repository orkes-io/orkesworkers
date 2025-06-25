package io.orkes.samples.workers;

import com.google.common.util.concurrent.Uninterruptibles;
import io.orkes.conductor.client.TaskClient;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class LongRunningAsyncTaskWorker {

    private TaskClient taskClient;
    private AtomicInteger integer = new AtomicInteger(0);
    private Map<String, TaskResult> taskResultsDataStore = new HashMap<>();

    public LongRunningAsyncTaskWorker(TaskClient taskClient) {
        this.taskClient = taskClient;
    }

    @Data
    public static class LongRunningTaskInput {
        private String workflowInstanceId;
        private String taskId;
        private int retryCount;
    }

    @WorkerTask("long_running_async_task")
    @Tool(description = "Executes a long running asynchronous task with periodic updates")
    public Map<String, Object> longRunningAsyncTask(
            @ToolParam(description = "Input parameters for long running async task") LongRunningTaskInput input) {

        String key = input.getWorkflowInstanceId() + "-" + input.getTaskId();

        // Create result map
        Map<String, Object> resultMap = new HashMap<>();

        if (taskResultsDataStore.containsKey(key)) {
            log.info("Task is already in progress - {}", key);
            TaskResult storedResult = taskResultsDataStore.get(key);
            return storedResult.getOutputData();
        }

        // Create a TaskResult for internal tracking
        TaskResult result = new TaskResult();
        result.setTaskId(input.getTaskId());
        result.setWorkflowInstanceId(input.getWorkflowInstanceId());
        result.setStatus(TaskResult.Status.IN_PROGRESS);
        result.setCallbackAfterSeconds(1200);
        result.getOutputData().clear();
        result.addOutputData("taskStatus", "STARTED");
        taskResultsDataStore.put(key, result);

        CompletableFuture.runAsync(() -> {
            if (input.getRetryCount() > 1 && input.getRetryCount() % 3 == 0) {
                for (int i = 0; i < 10; i++) {
                    Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
                    TaskResult taskResult = taskResultsDataStore.get(key);
                    String outKey = "runIt-" + i;
                    String value = Instant.now().toString();
                    taskResult.addOutputData(outKey, value);
                    taskResult.log(outKey + " - " + value);
                    taskResult.setStatus(TaskResult.Status.IN_PROGRESS);
                    log.info("Updating tasks offline - {}", taskResult);
                    taskClient.updateTask(taskResult);
                }
                TaskResult taskResult = taskResultsDataStore.get(key);
                taskResult.addOutputData("finalUpdate", "" + Instant.now().toString());
                taskResult.setCallbackAfterSeconds(0);
                taskResult.setStatus(TaskResult.Status.COMPLETED);
                taskClient.updateTask(taskResult);
            } else {
                TaskResult taskResult = taskResultsDataStore.get(key);
                taskResult.addOutputData("failureUpdate-" + integer.incrementAndGet(), "" + Instant.now().toString());
                taskResult.setStatus(TaskResult.Status.FAILED);
                taskClient.updateTask(taskResult);
            }
        });

        log.info("Returning result from main - {}", result);

        // Signal that this is an IN_PROGRESS task that needs special handling
        resultMap.put("taskStatus", "STARTED");
        resultMap.put("_specialHandling", "IN_PROGRESS");
        resultMap.put("_callbackAfterSeconds", 1200);

        return resultMap;
    }
}