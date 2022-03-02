package io.orkes.samples.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ExceptionTaskWorker implements Worker {
    @Override
    public String getTaskDefName() {
        return "always_throws_exception_worker";
    }

    @Override
    public TaskResult execute(Task task) {
        log.info("Retry count: {}", task.getRetryCount());
        throw new RuntimeException("I'm a task that always throws exceptions");
    }

}
