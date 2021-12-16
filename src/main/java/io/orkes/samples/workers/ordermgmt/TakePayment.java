package io.orkes.samples.workers.ordermgmt;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.springframework.stereotype.Component;

@Component
public class TakePayment implements Worker {
    @Override
    public String getTaskDefName() {
        return "take_payment";
    }

    @Override
    public TaskResult execute(Task task) {
        task.setStatus(Task.Status.COMPLETED);
        return new TaskResult(task);


    }
}
