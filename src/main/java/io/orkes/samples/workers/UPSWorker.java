package io.orkes.samples.workers;

import com.netflix.conductor.sdk.workflow.task.WorkerTask;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Component
public class UPSWorker {


    @Data
    public static class UPSWorkerInput {
    }

    @WorkerTask("ship_via_ups")
    @Tool(description = "Handles shipping via UPS")
    public Map<String, Object> executeUPSShipping(
            @ToolParam(description = "Input parameters for UPS shipping") UPSWorkerInput input) {

        Map<String, Object> result = new HashMap<>();
        // Since the original worker didn't do anything specific, we're keeping this simple
        return result;
    }
}