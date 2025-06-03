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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class DynamicTaskPreForkWorker {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Data
    public static class DynamicTaskPreForkInput {
        private List<Map<String, Object>> subWorkflowInputArray;
        private String subWorkflowName;
        private List<Map<String, Object>> taskInputs;
        private String taskName;
        private String taskType;
    }

    @WorkerTask("dynamic_task_prefork")
    @Tool(description = "Creates dynamic sub-workflow tasks based on input arrays")
    public Map<String, Object> executeDynamicTaskPrefork(
            @ToolParam(description = "Input parameters for dynamic task creation") DynamicTaskPreForkInput input) {

        Map<String, Object> result = new HashMap<>();

        try {
            List<Map<String, Object>> subWorkflowInputs = input.getSubWorkflowInputArray();
            String subWorkflowName = input.getSubWorkflowName();
            List<Map<String, Object>> taskInputs = input.getTaskInputs();
            String taskName = input.getTaskName();
            String taskType = input.getTaskType().toLowerCase();

            TaskType type = validateTaskTypes(taskType);

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
                for (String taskInputKey : subWorkflowInput.keySet()) {
                    dynamicTaskInput.put(taskInputKey, subWorkflowInput.get(taskInputKey));
                }

                dynamicTasksInput.put(taskRefName, dynamicTaskInput);
            }

            result.put("dynamicTasks", dynamicTasks);
            result.put("dynamicTasksInput", dynamicTasksInput);

        } catch (Exception e) {
            e.printStackTrace();

            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw, true);
            e.printStackTrace(pw);

            // Add error details to result
            result.put("error", sw.getBuffer().toString());
            throw new RuntimeException("Failed to create dynamic tasks: " + e.getMessage(), e);
        }

        return result;
    }

    public Set<TaskType> supportedTaskTypes = EnumSet.of(TaskType.SIMPLE, TaskType.SUB_WORKFLOW);

    private TaskType validateTaskTypes(String taskType) throws Exception {
        TaskType type = TaskType.of(taskType);
        if (!supportedTaskTypes.contains(type)) {
            throw new Exception("TaskType: " + taskType + " not supported. Supported types: " + supportedTaskTypes);
        }
        return type;
    }
}