package io.orkes.samples.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.springframework.stereotype.Component;

import java.util.UUID;


@Component
public class ShippingInfoWorker implements Worker {

    String postcode="95139";
    // in the above line we can read postcode from the database
    @Override
    public String getTaskDefName() {
        return "shipping_info";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        if(postcode.startsWith("9")==true) {
            result.addOutputData("shipping_service", "ship_via_fedex");
            result.log("shipped via fedex with unique id : " +UUID.randomUUID());
            // in the above line FedEx API can be called to fetch order related data
        }
        else {
            result.addOutputData("shipping_service", "ship_via_ups");
            result.log("shipped via ups with unique id : " +UUID.randomUUID());
            // in the above line UPS API can be called to fetch order related data
        }
        return result;
    }

}
