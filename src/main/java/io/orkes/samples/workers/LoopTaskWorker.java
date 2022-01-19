package io.orkes.samples.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.springframework.stereotype.Component;

@Component
public class LoopTaskWorker implements Worker {
    @Override
    public String getTaskDefName() {
        return "first_task_in_loop";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        result.addOutputData("outputVal", 1);
        result.addOutputData("status", "COMPLETED_WITH_ERRORS");
        result.setStatus(TaskResult.Status.COMPLETED);
        return result;
    }
}
