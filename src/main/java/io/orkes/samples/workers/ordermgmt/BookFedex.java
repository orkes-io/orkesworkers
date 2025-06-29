package io.orkes.samples.workers.ordermgmt;

import com.netflix.conductor.sdk.workflow.task.WorkerTask;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Component
public class BookFedex {

    @Data
    public static class BookFedexInput {
        // Empty class as the original worker didn't use any inputs
        // But maintained for consistency and potential future use
    }

    @WorkerTask("book_fedex")
    @Tool(description = "Books a FedEx shipment")
    public Map<String, Object> executeBookFedex(
            @ToolParam(description = "Input parameters for FedEx booking") BookFedexInput input) {

        //McpSchema.Tool
        // Since the original worker didn't do anything specific, we're keeping this simple
        return new HashMap<>();
    }
}