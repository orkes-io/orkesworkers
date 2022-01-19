package io.orkes.samples.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ShippingInfoWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "shipping_info";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        result.addOutputData("shipping_service", task.getInputData().get("service"));
        result.log("Shipped order reference id : " + UUID.randomUUID());
        result.setStatus(TaskResult.Status.COMPLETED);
        return result;
    }

}
