package io.orkes.samples.workers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.netflix.conductor.common.metadata.tasks.TaskType;
import com.netflix.conductor.common.metadata.workflow.SubWorkflowParams;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;
import lombok.Data;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class DynamicTaskArrayPreForkWorker {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Data
    public static class DynamicTaskArrayInput {
        private List<Map<String, Object>> inputsArray;
        private String subWorkflowName;
        private String taskName;
        private String taskType;
    }

    @WorkerTask("dynamic_task_array_prefork")
    @Tool(description = "Creates dynamic tasks or sub-workflows based on an array of inputs")
    public Map<String, Object> executeDynamicTaskArrayPrefork(
            @ToolParam(description = "Input parameters for dynamic task creation") DynamicTaskArrayInput input) {

        Map<String, Object> result = new HashMap<>();

        try {
            List<Map<String, Object>> inputs = input.getInputsArray();

            String subWorkflowName = input.getSubWorkflowName();
            String taskName = input.getTaskName();
            String taskType = input.getTaskType().toUpperCase();
            TaskType type = validateTaskTypes(taskType);

            List<WorkflowTask> dynamicTasks = Lists.newArrayList();
            Map<String, Object> dynamicTasksInput = Maps.newHashMap();

            int i = 0;
            String dynamicTaskName = (type == TaskType.SUB_WORKFLOW) ? subWorkflowName : taskName;
            for (Map<String, Object> inputItem : inputs) {
                String taskRefName = String.format("%s_%d", dynamicTaskName, i++);
                WorkflowTask dynamicTask = new WorkflowTask();
                dynamicTask.setName(dynamicTaskName);
                dynamicTask.setTaskReferenceName(taskRefName);

                if (type == TaskType.SUB_WORKFLOW) {
                    dynamicTask.setWorkflowTaskType(TaskType.SUB_WORKFLOW);
                    SubWorkflowParams subWorkflowParams = new SubWorkflowParams();
                    subWorkflowParams.setName(subWorkflowName);
                    dynamicTask.setSubWorkflowParam(subWorkflowParams);
                }

                dynamicTasks.add(dynamicTask);
                Map<String, Object> dynamicTaskInput = Maps.newHashMap();

                for (String inputKey : inputItem.keySet()) {
                    dynamicTaskInput.put(inputKey, inputItem.get(inputKey));
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
            throw new RuntimeException("Failed to create dynamic task array: " + e.getMessage(), e);
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