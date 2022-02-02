package io.orkes.samples.workers;

import com.google.common.util.concurrent.Uninterruptibles;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class LongRunningAsyncTaskWorker implements Worker {

    private TaskClient taskClient;

    public LongRunningAsyncTaskWorker(TaskClient taskClient) {
        this.taskClient = taskClient;
    }

    @Override
    public String getTaskDefName() {
        return "long_running_async_task";
    }

    @Override
    public TaskResult execute(Task task) {

        String key = task.getWorkflowInstanceId() + "-" + task.getTaskId();

        if(taskResultsDataStore.containsKey(key)) {
            log.info("Task is already in progress - {}", key);
            return taskResultsDataStore.get(key);
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.IN_PROGRESS);
        result.addOutputData("taskStatus", "STARTED");
        result.setCallbackAfterSeconds(1200);
        taskResultsDataStore.put(key, result);

        CompletableFuture.runAsync(() -> {
            for (int i = 0; i < 10; i++) {
                Uninterruptibles.sleepUninterruptibly(3, TimeUnit.SECONDS);
                TaskResult taskResult = taskResultsDataStore.get(key);
                taskResult.addOutputData("runIt-" + i, "" + Instant.now().toString());
                taskResult.setStatus(TaskResult.Status.IN_PROGRESS);
                log.info("Updating tasks offline - {}", taskResult);
                taskClient.updateTask(taskResult);
            }
            TaskResult taskResult = taskResultsDataStore.get(key);
            taskResult.addOutputData("finalUpdate", "" + Instant.now().toString());
            taskResult.setStatus(TaskResult.Status.COMPLETED);
            taskClient.updateTask(taskResult);
        });

        log.info("Returning result from main - {}", result);
        return result;
    }

    Map<String, TaskResult> taskResultsDataStore = new HashMap<>();

}
