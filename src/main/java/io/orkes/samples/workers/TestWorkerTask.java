package io.orkes.samples.workers;

import com.netflix.conductor.sdk.workflow.task.WorkerTask;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class TestWorkerTask {

    @WorkerTask("greet14")
    @Tool
    void execute(@ToolParam(description = "testing") String name) {
        System.out.println("hey world");
    }
}
