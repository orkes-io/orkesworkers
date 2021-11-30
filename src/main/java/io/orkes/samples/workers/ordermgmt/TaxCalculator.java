package io.orkes.samples.workers.ordermgmt;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.apache.logging.log4j.util.Strings;

import java.math.BigDecimal;

public class TaxCalculator implements Worker {
    @Override
    public String getTaskDefName() {
        return "calculate_tax";
    }

    @Override
    public TaskResult execute(Task task) {
        String zipCode = (String)task.getInputData().get("zipCode");
        if(zipCode == null || Strings.isEmpty(zipCode)) {
            task.setStatus(Task.Status.FAILED);
            task.setReasonForIncompletion("missing zip code");
        }
        double amount = (double)task.getInputData().get("amount");
        BigDecimal tax = new BigDecimal(amount).multiply(new BigDecimal(0.1));      //10% tax
        task.getOutputData().put("tax", tax);
        task.setStatus(Task.Status.COMPLETED);
        return new TaskResult(task);
    }
}
