package io.orkes.samples.workers;

import io.orkes.conductor.client.WorkflowClient;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.netflix.conductor.common.run.Workflow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;

@Component
public class WorkflowTerminateWorker implements Worker {
    @Override
    public String getTaskDefName() {
        return "workflow_terminate";
    }

    @Autowired
    private  WorkflowClient workflowClient;

//    private static final String CONDUCTOR_SERVER_URL = "conductor.server.url";

//    private WorkflowClient workflowClient() {
//        String rootUri = Environment.getProperty(CONDUCTOR_SERVER_URL);
//        log.info("Conductor Server URL: {}", rootUri);
//
//        WorkflowClient workflowClient = new WorkflowClient();
//        workflowClient.setRootURI(rootUri);
//
//        return workflowClient;
//    }
//    @Autowired
//    private TaskClient taskClient;


//    private WorkflowClient createClient() {
//        OrkesClients orkesClients = ApiUtil.getOrkesClient();
//        createMetadata();
//        WorkflowManagement workflowManagement = new WorkflowManagement();
//        workflowClient = orkesClients.getWorkflowClient();
//    }
    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);

        try {
            String workflowId = (String) task.getInputData().get("workflowId");
            String reason = (String) task.getInputData().get("reason");
            Workflow workflow = workflowClient.getWorkflow(workflowId, false);
            workflowClient.terminateWorkflow(workflowId, reason);
            result.addOutputData("workflowStatusBeforeTermination", workflow.getStatus());
            result.setStatus(TaskResult.Status.COMPLETED);

        } catch (Exception e) {
            e.printStackTrace();
            result.setStatus(TaskResult.Status.FAILED);
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw, true);
            e.printStackTrace(pw);
            result.log(sw.getBuffer().toString());
        }
        return result;
    }
}
