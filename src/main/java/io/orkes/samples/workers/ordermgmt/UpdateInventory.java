package io.orkes.samples.workers.ordermgmt;

import com.netflix.conductor.sdk.workflow.task.WorkerTask;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import lombok.Data;
import java.util.HashMap;
import java.util.Map;

@Component
public class UpdateInventory {

    @Data
    public static class UpdateInventoryInput {
    }

    @WorkerTask("update_inventory")
    @Tool(description = "Updates inventory levels after order processing")
    public Map<String, Object> executeUpdateInventory(
            @ToolParam(description = "Input parameters for inventory update") UpdateInventoryInput input) {

        // Since the original worker didn't do anything specific, we're keeping this simple
        return new HashMap<>();
    }
}