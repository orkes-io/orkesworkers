package io.orkes.samples.workers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.netflix.conductor.common.metadata.tasks.TaskType;
import com.netflix.conductor.common.metadata.workflow.SubWorkflowParams;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import io.orkes.samples.utils.Size;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class TerminateCleanupPreforkWorker implements Worker {

    private ObjectMapper objectMapper = new ObjectMapper();


    @Override
    public String getTaskDefName() {
        return "terminate_cleanup_prefork";
    }

    @Override
    public TaskResult execute(Task task) {

        TaskResult result = new TaskResult(task);

        try {


            List<Map<String, Object>> subWorkflowInputs = (List<Map<String, Object>>) task.getInputData().get("subWorkflowInputArray");
            String subWorkflowName = (String)(task.getInputData().get("subWorkflowName"));


            List<WorkflowTask> dynamicTasks =  Lists.newArrayList();
            Map<String, Object> dynamicTasksInput = Maps.newHashMap();

            int i=0;
            String dynamicTaskName = subWorkflowName;
            for (Map<String, Object> subWorkflowInput :
                    subWorkflowInputs) {

                    String taskRefName = String.format("%s_%d",dynamicTaskName, i++);
                    WorkflowTask dynamicTask = new WorkflowTask();
                    dynamicTask.setName(dynamicTaskName);
                    dynamicTask.setTaskReferenceName(taskRefName);
                    dynamicTask.setWorkflowTaskType(TaskType.SUB_WORKFLOW);
                    SubWorkflowParams subWorkflowParams = new SubWorkflowParams();
                    subWorkflowParams.setName(subWorkflowName);
                    dynamicTask.setSubWorkflowParam(subWorkflowParams);
                    dynamicTasks.add(dynamicTask);

                    Map<String, Object> dynamicTaskInput = Maps.newHashMap();
                    dynamicTaskInput.put("workflowId", subWorkflowInput.get("workflowId"));
                    dynamicTaskInput.put("reason", subWorkflowInput.get("reason"));

                    dynamicTasksInput.put(taskRefName,dynamicTaskInput );
            }


            result.setStatus(TaskResult.Status.COMPLETED);
            String currentTimeOnServer = Instant.now().toString();
            result.log("This is a test log at time: " + currentTimeOnServer);
            result.addOutputData("dynamicTasks", dynamicTasks);
            result.addOutputData("dynamicTasksInput", dynamicTasksInput);

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