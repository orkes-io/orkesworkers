package io.orkes.samples.workers;

import com.netflix.conductor.sdk.workflow.task.WorkerTask;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import lombok.Data;

@Component
public class ReadNameWorker {

    @Data
    public static class ReadNameInput {

    }

    @WorkerTask("Read_Name")
    @Tool(description = "Reads a name")
    public void readName(
            @ToolParam(description = "Input parameters for reading a name") ReadNameInput input) {
    }
}