package io.orkes.samples.workers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.netflix.conductor.common.metadata.tasks.TaskType;
import com.netflix.conductor.common.metadata.workflow.SubWorkflowParams;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;
import io.orkes.samples.utils.Size;
import lombok.Data;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TerminateCleanupPreforkWorker {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Data
    public static class TerminateCleanupPreforkInput {
        private List<Map<String, Object>> subWorkflowInputArray;
        private String subWorkflowName;
    }

    @WorkerTask("terminate_cleanup_prefork")
    @Tool(description = "Creates dynamic sub-workflow tasks for termination and cleanup operations")
    public Map<String, Object> executeTerminateCleanupPrefork(
            @ToolParam(description = "Input parameters for termination and cleanup") TerminateCleanupPreforkInput input) {

        Map<String, Object> result = new HashMap<>();

        try {
            List<Map<String, Object>> subWorkflowInputs = input.getSubWorkflowInputArray();
            String subWorkflowName = input.getSubWorkflowName();

            List<WorkflowTask> dynamicTasks = Lists.newArrayList();
            Map<String, Object> dynamicTasksInput = Maps.newHashMap();

            int i = 0;
            String dynamicTaskName = subWorkflowName;
            for (Map<String, Object> subWorkflowInput : subWorkflowInputs) {
                String taskRefName = String.format("%s_%d", dynamicTaskName, i++);
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

                dynamicTasksInput.put(taskRefName, dynamicTaskInput);
            }

            String currentTimeOnServer = Instant.now().toString();

            // Add outputs to result
            result.put("dynamicTasks", dynamicTasks);
            result.put("dynamicTasksInput", dynamicTasksInput);
            result.put("log", "This is a test log at time: " + currentTimeOnServer);

        } catch (Exception e) {
            e.printStackTrace();
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw, true);
            e.printStackTrace(pw);

            // Add error details to result
            result.put("error", sw.getBuffer().toString());
            throw new RuntimeException("Failed to execute terminate cleanup prefork: " + e.getMessage(), e);
        }

        return result;
    }
}