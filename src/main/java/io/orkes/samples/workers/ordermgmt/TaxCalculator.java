package io.orkes.samples.workers.ordermgmt;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TaxCalculator implements Worker {
    @Override
    public String getTaskDefName() {
        return "calculate_tax";
    }

    @Override
    public TaskResult execute(Task task) {
        String zipCode = (String)task.getInputData().get("zipCode");
        if(zipCode == null || StringUtils.isEmpty(zipCode)) {
            task.setStatus(Task.Status.FAILED);
            task.setReasonForIncompletion("missing zip code");
        }
        Number amount = (Number)task.getInputData().get("amount");
        if(amount == null) {
            task.setStatus(Task.Status.FAILED);
            task.setReasonForIncompletion("missing amount");
        }
        BigDecimal tax = new BigDecimal(amount.doubleValue()).multiply(new BigDecimal(0.1));      //10% tax
        task.getOutputData().put("tax", tax);
        task.setStatus(Task.Status.COMPLETED);
        return new TaskResult(task);
    }
}
