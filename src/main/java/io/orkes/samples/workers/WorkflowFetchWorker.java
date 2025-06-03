package io.orkes.samples.workers;

import com.netflix.conductor.sdk.workflow.task.WorkerTask;
import io.orkes.conductor.client.WorkflowClient;
import com.netflix.conductor.common.run.Workflow;
import lombok.Data;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class WorkflowFetchWorker {

    @Data
    public static class WorkflowFetchInput {
        private String correlationId;
        private String name;
        private String includeClosed;
        private String includeTasks;
    }

    @Autowired
    private WorkflowClient client;

    @WorkerTask("workflow_fetch")
    @Tool(description = "Fetches workflows based on correlation ID and name")
    public Map<String, Object> executeWorkflowFetch(
            @ToolParam(description = "Input parameters for workflow fetching") WorkflowFetchInput input) {

        Map<String, Object> result = new HashMap<>();

        try {
            String correlationId = input.getCorrelationId();
            String name = input.getName();

            Boolean includeClosed = true;
            if (input.getIncludeClosed() != null) {
                includeClosed = Boolean.valueOf(input.getIncludeClosed());
            }

            Boolean includeTasks = false;
            if (input.getIncludeTasks() != null) {
                includeTasks = Boolean.valueOf(input.getIncludeTasks());
            }

            List<Workflow> workflows = client.getWorkflows(name, correlationId, includeClosed, includeTasks);
            result.put("workflows", workflows);

        } catch (Exception e) {
            e.printStackTrace();

            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw, true);
            e.printStackTrace(pw);

            // Add error details to result
            result.put("error", sw.getBuffer().toString());
            throw new RuntimeException("Failed to fetch workflows: " + e.getMessage(), e);
        }

        return result;
    }
}