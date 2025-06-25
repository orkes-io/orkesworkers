package io.orkes.samples.workers;

import com.netflix.conductor.sdk.workflow.task.WorkerTask;
import lombok.Data;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class FedExWorker {

    @Data
    public static class FedExInput {

    }

    @WorkerTask("ship_via_fedex")
    @Tool(description = "Ship a package via FedEx")
    public void shipViaFedEx(
            @ToolParam(description = "Input parameters") FedExInput input) {

    }
}