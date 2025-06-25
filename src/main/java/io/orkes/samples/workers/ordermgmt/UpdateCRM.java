package io.orkes.samples.workers.ordermgmt;

import com.netflix.conductor.sdk.workflow.task.WorkerTask;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Component
public class UpdateCRM {

    @Data
    public static class UpdateCRMInput {

    }

    @WorkerTask("update_crm")
    @Tool(description = "Updates customer information in the CRM system")
    public Map<String, Object> executeUpdateCRM(
            @ToolParam(description = "Input parameters for CRM update") UpdateCRMInput input) {

        return new HashMap<>();
    }
}