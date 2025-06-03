package io.orkes.samples.workers;

import io.orkes.conductor.client.WorkflowClient;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;
import com.netflix.conductor.common.run.Workflow;
import lombok.Data;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

@Component
public class WorkflowTerminateWorker {

    @Data
    public static class WorkflowTerminateInput {
        private String workflowId;
        private String reason;
    }

    @Autowired
    private WorkflowClient workflowClient;

    // Note: Commented code from original class is preserved below
    // private static final String CONDUCTOR_SERVER_URL = "conductor.server.url";

    // private WorkflowClient workflowClient() {
    //     String rootUri = Environment.getProperty(CONDUCTOR_SERVER_URL);
    //     log.info("Conductor Server URL: {}", rootUri);
    //
    //     WorkflowClient workflowClient = new WorkflowClient();
    //     workflowClient.setRootURI(rootUri);
    //
    //     return workflowClient;
    // }
    // @Autowired
    // private TaskClient taskClient;

    // private WorkflowClient createClient() {
    //     OrkesClients orkesClients = ApiUtil.getOrkesClient();
    //     createMetadata();
    //     WorkflowManagement workflowManagement = new WorkflowManagement();
    //     workflowClient = orkesClients.getWorkflowClient();
    // }

    @WorkerTask("workflow_terminate")
    @Tool(description = "Terminates a workflow by ID with a specified reason")
    public Map<String, Object> executeWorkflowTerminate(
            @ToolParam(description = "Input parameters for workflow termination") WorkflowTerminateInput input) {

        Map<String, Object> result = new HashMap<>();

        try {
            String workflowId = input.getWorkflowId();
            String reason = input.getReason();

            Workflow workflow = workflowClient.getWorkflow(workflowId, false);
            workflowClient.terminateWorkflow(workflowId, reason);

            result.put("workflowStatusBeforeTermination", workflow.getStatus());

        } catch (Exception e) {
            e.printStackTrace();

            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw, true);
            e.printStackTrace(pw);

            // Add error details to result
            result.put("error", sw.getBuffer().toString());
            throw new RuntimeException("Failed to terminate workflow: " + e.getMessage(), e);
        }

        return result;
    }
}