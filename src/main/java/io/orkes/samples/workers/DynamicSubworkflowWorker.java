package io.orkes.samples.workers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.netflix.conductor.common.metadata.workflow.StartWorkflowRequest;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.sdk.workflow.def.ConductorWorkflow;
import com.netflix.conductor.sdk.workflow.def.tasks.*;
import com.netflix.conductor.sdk.workflow.executor.WorkflowExecutor;
import io.orkes.conductor.client.WorkflowClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@AllArgsConstructor
public class DynamicSubworkflowWorker implements Worker {

    private final WorkflowClient workflowClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WorkflowExecutor executor;

    @Override
    public String getTaskDefName() {
        return "dynamic_subworkflow_task";
    }

    /**
     * This Worker will start 'dynamic_workflow' workflow and pass the subworkflow definitions using createDynamicSubworkflow() method
     * @param task
     * @return
     */
    @Override
    public TaskResult execute(Task task) {
        System.out.println("Starting dynamic_subworkflow_task");
        TaskResult result = new TaskResult(task);
        try {
            result.setOutputData(startQuestWorkflow());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        result.setStatus(TaskResult.Status.COMPLETED);
        return result;
    }

    public Map<String, Object> startQuestWorkflow() throws JsonProcessingException {
        StartWorkflowRequest request = new StartWorkflowRequest();
        request.setName("dynamic_workflow");
        Map<String, Object> inputData = new HashMap<>();
        //inputData.put("enrichmentSubworkflowsDef", subworkflowDef());
        Object dynamicSubworkflowDef = objectMapper.convertValue(createDynamicSubworkflow(), Object.class);
        inputData.put("dynamicSubworkflowDef", dynamicSubworkflowDef);
        request.setInput(inputData);

        String workflowId = workflowClient.startWorkflow(request);
        log.info("Workflow id: {}", workflowId);
        return Map.of("workflowId", workflowId);
    }

    private WorkflowDef createDynamicSubworkflow() {
        var workflow = new ConductorWorkflow<>(executor);
        workflow.setName("dynamic_subworkflows_series");
        workflow.setVersion(1);
        workflow.setOwnerEmail("saksham.solanki@orkes.io");
        workflow.setDescription("test");
        workflow.setVariables(Map.of());
        workflow.setDefaultInput(Map.of());
        workflow.setTimeoutPolicy(WorkflowDef.TimeoutPolicy.ALERT_ONLY);

        // ---- Fork task def started
        com.netflix.conductor.sdk.workflow.def.tasks.Task[][] forkedTasks = new com.netflix.conductor.sdk.workflow.def.tasks.Task[4][1];

        //Below code is subworkflows in forked task
        //ForkTask is having following structure [{}}, {}}]
        //Enrichment level started
        ConductorWorkflow conductorWorkflow = new ConductorWorkflow(executor);
        conductorWorkflow.setName("imdb_enrichment_workflow");
        Http httptask = new Http("imdb_enrichment_workflow_task");
        httptask.url("https://orkes-api-tester.orkesconductor.com/api");
        conductorWorkflow.add(httptask);

        SubWorkflow forkSubworkflow = new SubWorkflow("imdb_enrichment_subworkflow", conductorWorkflow);
        forkSubworkflow.input("name","{workflow.input.name}");
        forkedTasks[0][0] = forkSubworkflow;

        conductorWorkflow = new ConductorWorkflow(executor);
        conductorWorkflow.setName("empi_enrichment_workflow");
        httptask = new Http("empi_enrichment_workflow_task");
        httptask.url("https://orkes-api-tester.orkesconductor.com/api");
        conductorWorkflow.add(httptask);

        forkSubworkflow = new SubWorkflow("empi_enrichment_subworkflow", conductorWorkflow);
        forkedTasks[1][0] = forkSubworkflow;


        conductorWorkflow = new ConductorWorkflow(executor);
        conductorWorkflow.setName("mlcp_enrichment_workflow");
        httptask = new Http("mlcp_enrichment_workflow_task");
        httptask.url("https://orkes-api-tester.orkesconductor.com/api");
        conductorWorkflow.add(httptask);

        forkSubworkflow = new SubWorkflow("mlcp_enrichment_workflow", conductorWorkflow);
        forkedTasks[2][0] = forkSubworkflow;


        conductorWorkflow = new ConductorWorkflow(executor);
        conductorWorkflow.setName("ohc_enrichment_workflow");
        httptask = new Http("ohc_enrichment_workflow_task");
        httptask.url("https://orkes-api-tester.orkesconductor.com/api");
        conductorWorkflow.add(httptask);

        forkSubworkflow = new SubWorkflow("ohc_enrichment_subworkflow", conductorWorkflow);
        forkedTasks[3][0] = forkSubworkflow;

        ForkJoin forkJoin = new ForkJoin("fork_enrichment", forkedTasks);
        workflow.add(forkJoin);
        // Enrichment level ended


        // Translation Level Starts
        com.netflix.conductor.sdk.workflow.def.tasks.Task[][] forkedTasks1 = new com.netflix.conductor.sdk.workflow.def.tasks.Task[2][1];
        conductorWorkflow = new ConductorWorkflow(executor);
        conductorWorkflow.setName("Labos_Translation_workflow");
        httptask = new Http("IMDB_EMPI_Translations");
        httptask.url("https://orkes-api-tester.orkesconductor.com/api");
        conductorWorkflow.add(httptask);

        httptask = new Http("LabOS_Translation");
        httptask.url("https://orkes-api-tester.orkesconductor.com/api");
        conductorWorkflow.add(httptask);


        forkSubworkflow = new SubWorkflow("LabOS_Translation_subworkflow", conductorWorkflow);
        forkedTasks1[0][0] = forkSubworkflow;

        conductorWorkflow = new ConductorWorkflow(executor);
        conductorWorkflow.setName("BFE_Translation_workflow");
        httptask = new Http("IMDB_Enrichment");
        httptask.url("https://orkes-api-tester.orkesconductor.com/api");
        conductorWorkflow.add(httptask);

        httptask = new Http("BFE_Translation");
        httptask.url("https://orkes-api-tester.orkesconductor.com/api");
        conductorWorkflow.add(httptask);

        forkSubworkflow = new SubWorkflow("BFE_Translation_subworkflow", conductorWorkflow);
        forkedTasks1[1][0] = forkSubworkflow;

        forkJoin = new ForkJoin("fork_translation", forkedTasks1);
        workflow.add(forkJoin);
        //Translation Level Ended

        //Distribution level starts
        com.netflix.conductor.sdk.workflow.def.tasks.Task[][] forkedTasks2 = new com.netflix.conductor.sdk.workflow.def.tasks.Task[3][1];
        conductorWorkflow = new ConductorWorkflow(executor);
        conductorWorkflow.setName("BFE_Distributions");
        httptask = new Http("bfe_distributions_subworkflow_task");
        httptask.url("https://orkes-api-tester.orkesconductor.com/api");
        conductorWorkflow.add(httptask);

        forkSubworkflow = new SubWorkflow("BFE_Distributions_subworkflow", conductorWorkflow);
        forkSubworkflow.input("name","{workflow.input.name}");
        forkedTasks2[0][0] = forkSubworkflow;

        conductorWorkflow = new ConductorWorkflow(executor);
        conductorWorkflow.setName("ELabs_Distributions");
        httptask = new Http("ELabs_Distributions_subworkflow_task");
        httptask.url("https://orkes-api-tester.orkesconductor.com/api");
        conductorWorkflow.add(httptask);

        forkSubworkflow = new SubWorkflow("ELabs_Distributions_subworkflow", conductorWorkflow);
        forkedTasks2[1][0] = forkSubworkflow;


        conductorWorkflow = new ConductorWorkflow(executor);
        conductorWorkflow.setName("LabOs_Distributions");
        httptask = new Http("LabOs_Distributions_subworkflow_task");
        httptask.url("https://orkes-api-tester.orkesconductor.com/api");
        conductorWorkflow.add(httptask);

        forkSubworkflow = new SubWorkflow("LabOs_Distributions_subworkflow", conductorWorkflow);
        forkedTasks2[2][0] = forkSubworkflow;

        forkJoin = new ForkJoin("fork_distribution", forkedTasks2);
        workflow.add(forkJoin);

        // Distribution level Ended

        WorkflowDef workflowDef = workflow.toWorkflowDef();
        workflowDef.setOutputParameters(Map.of());
        workflowDef.setTimeoutSeconds(0);
        workflowDef.setInputTemplate(Map.of());
        workflowDef.setSchemaVersion(2);
        workflowDef.setInputParameters(List.of());

        return workflowDef;
    }
}
