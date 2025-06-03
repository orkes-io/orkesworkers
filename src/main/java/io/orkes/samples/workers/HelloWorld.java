package io.orkes.samples.workers;

import com.netflix.conductor.sdk.workflow.task.WorkerTask;
import com.netflix.conductor.sdk.workflow.task.OutputParam;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import lombok.Data;

@Component
public class HelloWorld {

    // POJO for input parameters (empty as the original worker didn't use any inputs)
    @Data
    public static class HelloWorldInput {
        // Empty class as no inputs were used in the original implementation
    }

    @WorkerTask("hello_world")
    @Tool(description = "Returns a Hello World greeting")
    public @OutputParam("hw_response") String helloWorld(
            @ToolParam(description = "Input parameters") HelloWorldInput input) {
        return "Hello World!";
    }
}