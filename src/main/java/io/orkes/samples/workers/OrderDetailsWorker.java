package io.orkes.samples.workers;

import com.netflix.conductor.sdk.workflow.task.WorkerTask;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import lombok.Data;

@Component
public class OrderDetailsWorker {

    @Data
    public static class OrderDetailsInput {
        // Empty class as the original worker didn't use any inputs
        // But maintained for consistency and potential future use
    }

    @WorkerTask("order_details")
    @Tool(description = "Processes order details")
    public void processOrderDetails(
            @ToolParam(description = "Input parameters for order details") OrderDetailsInput input) {
        // The original implementation just completes successfully without any logic
        // So we keep the same behavior - just return normally with no output
    }
}