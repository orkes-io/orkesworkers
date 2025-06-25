package io.orkes.samples.workers;

import com.netflix.conductor.sdk.workflow.task.WorkerTask;
import com.netflix.conductor.sdk.workflow.task.OutputParam;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class ShippingInfoWorker {

    @Data
    public static class ShippingInfoInput {
        private String service;
    }

    @WorkerTask("shipping_info")
    @Tool(description = "Processes shipping information for an order")
    public Map<String, Object> processShippingInfo(
            @ToolParam(description = "Input parameters for shipping") ShippingInfoInput input) {

        Map<String, Object> result = new HashMap<>();

        // Add the shipping service to output data
        result.put("shipping_service", input.getService());

        // Generate and log UUID
        String referenceId = UUID.randomUUID().toString();
        // Include log in output
        result.put("log", "Shipped order reference id : " + referenceId);

        return result;
    }
}