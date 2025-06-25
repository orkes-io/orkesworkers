package io.orkes.samples.workers.ordermgmt;

import com.netflix.conductor.sdk.workflow.task.WorkerTask;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Component
public class TakePayment {

    @Data
    public static class TakePaymentInput {
        // Empty class as the original worker didn't use any inputs
        // But maintained for consistency and potential future use
    }

    @WorkerTask("take_payment")
    @Tool(description = "Processes payment for orders")
    public Map<String, Object> executeTakePayment(
            @ToolParam(description = "Input parameters for payment processing") TakePaymentInput input) {

        // Since the original worker didn't do anything specific, we're keeping this simple
        return new HashMap<>();
    }
}