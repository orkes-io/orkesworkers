package io.orkes.samples;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ConductorModeler {

    public static void main(String[] args) throws JsonProcessingException {
        WorkflowDef def = new WorkflowDef();
        def.setDescription("Test Looping");
        def.setName("test_looping_concurrency");
        def.setOwnerEmail("builds@orkes.io");
        List<WorkflowTask> workflowTasks = new ArrayList<>();
        WorkflowTask doWhileTask = new WorkflowTask();
        doWhileTask.setName("loop");
        doWhileTask.setType("DO_WHILE");
        Map<String, Object> params = new HashMap<>();
        params.put("value", "${workflow.input.value}");
        doWhileTask.setLoopCondition("(($.loop['iteration'] < $.value ) || ( $.first_task['outputVal'] > 10))");
        List<WorkflowTask> loopTasks = new ArrayList<>();
        WorkflowTask firstTask = new WorkflowTask();
        firstTask.setName("first_task_in_loop");
        firstTask.setTaskReferenceName("first_task_in_loop");
        firstTask.setType("SIMPLE");
        loopTasks.add(firstTask);
        doWhileTask.setLoopOver(loopTasks);
        doWhileTask.setInputParameters(params);
        workflowTasks.add(doWhileTask);
        def.setTasks(workflowTasks);
        log.info(new ObjectMapper().writeValueAsString(def));
    }
}
