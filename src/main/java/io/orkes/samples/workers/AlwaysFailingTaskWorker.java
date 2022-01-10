package io.orkes.samples.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.springframework.stereotype.Component;

@Component
public class AlwaysFailingTaskWorker implements Worker {
    @Override
    public String getTaskDefName() {
        return "always_failing_task";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        result.addOutputData("outputVal", "This task always fails (for testing)");
        result.setStatus(TaskResult.Status.FAILED);
        return result;
    }
}
