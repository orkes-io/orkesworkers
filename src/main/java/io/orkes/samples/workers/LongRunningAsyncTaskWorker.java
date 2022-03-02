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
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class LongRunningAsyncTaskWorker implements Worker {

    private TaskClient taskClient;
    private AtomicInteger integer = new AtomicInteger(0);

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
        result.setCallbackAfterSeconds(1200);
        result.getOutputData().clear();
        result.addOutputData("taskStatus", "STARTED");
        taskResultsDataStore.put(key, result);

        CompletableFuture.runAsync(() -> {

            if(task.getRetryCount() > 1 && task.getRetryCount() % 3 == 0) {
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
        return result;
    }

    Map<String, TaskResult> taskResultsDataStore = new HashMap<>();

}
