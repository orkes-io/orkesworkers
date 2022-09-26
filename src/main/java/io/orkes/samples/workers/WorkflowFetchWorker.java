package io.orkes.samples.workers;


import com.netflix.conductor.client.worker.Worker;
import io.orkes.conductor.client.WorkflowClient;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.netflix.conductor.common.run.Workflow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

@Component
public class WorkflowFetchWorker implements Worker {
    @Override
    public String getTaskDefName() {
        return "workflow_fetch";
    }

    @Autowired
    private  WorkflowClient client;

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);

        try {
            String correlationId = (String) task.getInputData().get("correlationId");
            String name = (String) task.getInputData().get("name");

            Boolean includeClosed = true;
            if(task.getInputData().containsKey("includeClosed")) {
                includeClosed = Boolean.valueOf((String)task.getInputData().get("includeClosed"));
            }

            Boolean includeTasks = false;
            if(task.getInputData().containsKey("includeTasks")) {
                includeTasks = Boolean.valueOf((String)task.getInputData().get("includeTasks"));
            }

            List<Workflow> workflows = client.getWorkflows(name, correlationId, includeClosed, includeTasks);
            result.addOutputData("workflows", workflows);
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
