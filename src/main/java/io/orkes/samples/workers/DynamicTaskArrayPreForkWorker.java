package io.orkes.samples.workers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.netflix.conductor.common.metadata.tasks.TaskType;
import com.netflix.conductor.common.metadata.workflow.SubWorkflowParams;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class DynamicTaskArrayPreForkWorker implements Worker {

    private ObjectMapper objectMapper = new ObjectMapper();


    @Override
    public String getTaskDefName() {
        return "dynamic_task_array_prefork";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);

        try {

            List<Map<String, Object>> inputs = (List<Map<String, Object>>) task.getInputData().get("inputsArray");

            String subWorkflowName = null;
            if(task.getInputData().containsKey("subWorkflowName")) {
                subWorkflowName = (String)(task.getInputData().get("subWorkflowName"));
            }

            String taskName = null;
            if(task.getInputData().containsKey("taskName")) {
                taskName = (String) (task.getInputData().get("taskName"));
            }

            String taskType = ((String) task.getInputData().get("taskType")).toUpperCase();
            TaskType type = validateTaskTypes(taskType);


            List<WorkflowTask> dynamicTasks =  Lists.newArrayList();
            Map<String, Object> dynamicTasksInput = Maps.newHashMap();

            int i=0;
            String dynamicTaskName = (type == TaskType.SUB_WORKFLOW) ? subWorkflowName : taskName;
            for (Map<String, Object> input :
                    inputs) {


                String taskRefName = String.format("%s_%d",dynamicTaskName, i++);
                WorkflowTask dynamicTask = new WorkflowTask();
                dynamicTask.setName(dynamicTaskName);
                dynamicTask.setTaskReferenceName(taskRefName);

                if(type == TaskType.SUB_WORKFLOW) {
                    dynamicTask.setWorkflowTaskType(TaskType.SUB_WORKFLOW);
                    SubWorkflowParams subWorkflowParams = new SubWorkflowParams();
                    subWorkflowParams.setName(subWorkflowName);
                    dynamicTask.setSubWorkflowParam(subWorkflowParams);
                }

                dynamicTasks.add(dynamicTask);
                Map<String, Object> dynamicTaskInput = Maps.newHashMap();

                for (String inputKey:
                        input.keySet()) {
                    dynamicTaskInput.put(inputKey, input.get(inputKey));
                }


                dynamicTasksInput.put(taskRefName,dynamicTaskInput);
            }

            result.setStatus(TaskResult.Status.COMPLETED);
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


    public Set<TaskType> supportedTaskTypes = EnumSet.of(TaskType.SIMPLE, TaskType.SUB_WORKFLOW);


    private TaskType validateTaskTypes(String taskType) throws Exception {
        TaskType type = TaskType.of(taskType);
        if(!supportedTaskTypes.contains(type)) {
            throw new Exception("TaskType: " + taskType + " not supported. Supported types: " + supportedTaskTypes);
        }
        return type;
    }

}