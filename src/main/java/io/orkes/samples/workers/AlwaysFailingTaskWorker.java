package io.orkes.samples.workers;

import com.netflix.conductor.sdk.workflow.task.OutputParam;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class AlwaysFailingTaskWorker implements MCPTool {

    @WorkerTask("always_failing_task")
    @Tool(description = "A task that always fails (for testing purposes)")
    public @OutputParam("outputVal") String executeAlwaysFailingTask() {
        return "This task always fails (for testing)";
    }
}
