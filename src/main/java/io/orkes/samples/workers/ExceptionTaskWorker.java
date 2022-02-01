package io.orkes.samples.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.springframework.stereotype.Component;

@Component
public class ExceptionTaskWorker implements Worker {
    @Override
    public String getTaskDefName() {
        return "always_throws_exception_worker";
    }

    @Override
    public TaskResult execute(Task task) {
        throw new RuntimeException("I'm a task that always throws exceptions");
    }
}
