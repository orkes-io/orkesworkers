package io.orkes.samples.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class TaskCallbackWorker implements Worker {
    @Override
    public String getTaskDefName() {
        return "test_task_callback";
    }

    private AtomicInteger runCount = new AtomicInteger(0);

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        result.addOutputData("result" + runCount.incrementAndGet(), "Run Count: "  + runCount.get());
        result.log("Running for " + runCount.get());
        if(runCount.get() % 5 == 0) { // Check DB if things are completed
            result.setStatus(TaskResult.Status.COMPLETED);
        } else { // If still running then post updates
            result.setStatus(TaskResult.Status.IN_PROGRESS);
            result.setCallbackAfterSeconds(3);
        }
        return result;
    }
}
