package io.orkes.samples.workers;

import com.netflix.conductor.sdk.workflow.task.WorkerTask;
import com.netflix.conductor.sdk.workflow.task.OutputParam;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Component
public class LoopTaskWorker {

    @Data
    public static class LoopTaskInput {
        // Empty class as the original worker didn't use any inputs
        // But maintained for consistency and potential future use
    }

    @WorkerTask("first_task_in_loop")
    @Tool(description = "Executes the first task in a loop operation")
    public Map<String, Object> executeFirstTaskInLoop(
            @ToolParam(description = "Input parameters") LoopTaskInput input) {

        Map<String, Object> result = new HashMap<>();
        result.put("outputVal", 1);
        result.put("status", "COMPLETED_WITH_ERRORS");

        return result;
    }
}