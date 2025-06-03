package io.orkes.samples.workers.ordermgmt;

import com.netflix.conductor.sdk.workflow.task.WorkerTask;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Component
public class RequestPickAndPack {

    @Data
    public static class RequestPickAndPackInput {
        // Empty class as the original worker didn't use any inputs
        // But maintained for consistency and potential future use
    }

    @WorkerTask("request_pick_and_pack")
    @Tool(description = "Handles pick and pack requests for order fulfillment")
    public Map<String, Object> executeRequestPickAndPack(
            @ToolParam(description = "Input parameters for pick and pack request") RequestPickAndPackInput input) {

        // Since the original worker didn't do anything specific, we're keeping this simple
        return new HashMap<>();
    }
}