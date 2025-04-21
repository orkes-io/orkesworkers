package io.orkes.samples.workers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.sdk.workflow.def.ConductorWorkflow;
import com.netflix.conductor.sdk.workflow.def.tasks.*;
import com.netflix.conductor.sdk.workflow.executor.WorkflowExecutor;
import io.orkes.samples.models.MediationRules;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


import java.util.List;
import java.util.Map;

@Component
@Slf4j
@AllArgsConstructor
public class DynamicSubworkflowWorker implements Worker {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WorkflowExecutor executor;

    @Override
    public String getTaskDefName() {
        return "create_dynamic_workflow_def";
    }

    /**
     * This Worker will start 'dynamic_workflow' workflow and pass the subworkflow definitions using createDynamicSubworkflow() method
     * @param task
     * @return
     */
    @Override
    public TaskResult execute(Task task) {
        System.out.println("Starting create_dynamic_workflow_def task");
        TaskResult result = new TaskResult(task);
        try {
            MediationRules mediationRules = objectMapper.convertValue(task.getInputData().get("mediation_rules"), MediationRules.class);
            result.setOutputData(startExistingWorkflow(mediationRules));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        result.setStatus(TaskResult.Status.COMPLETED);
        return result;
    }

    public Map<String, Object> startExistingWorkflow(MediationRules mediationRules) throws JsonProcessingException {
        Object dynamicSubworkflowDef = objectMapper.convertValue(createDynamicSubworkflow(mediationRules), Object.class);
        return Map.of("workflow_def", dynamicSubworkflowDef);
    }

    private WorkflowDef createDynamicSubworkflow(MediationRules mediationRules) throws JsonProcessingException {
        var workflow = new ConductorWorkflow<>(executor);
        workflow.setName("dynamic_subworkflows_series");
        workflow.setVersion(1);
        workflow.setOwnerEmail("saksham.solanki@orkes.io");
        workflow.setDescription("test");
        workflow.setVariables(Map.of());
        workflow.setDefaultInput(Map.of());
        workflow.setTimeoutPolicy(WorkflowDef.TimeoutPolicy.ALERT_ONLY);

        //ForkTask is having following structure [{}, {}]

        // --------------- Enrichment level started ------------------
        com.netflix.conductor.sdk.workflow.def.tasks.Task[][] enrichmentForkTasks = new com.netflix.conductor.sdk.workflow.def.tasks.Task[mediationRules.getEnrichments().size()][1];
        for (int i = 0; i < mediationRules.getEnrichments().size(); i++) {
            SubWorkflow forkSubworkflow = new SubWorkflow(mediationRules.getEnrichments().get(i).getEnrichmentType() + "_subworkflow_ref", "OP_" + mediationRules.getEnrichments().get(i).getEnrichmentType(), 1);
            forkSubworkflow.input("sendToORB", mediationRules.getEnrichments().get(i).getOrbpFlags());
            enrichmentForkTasks[i][0] = forkSubworkflow;
        }
        ForkJoin forkEnrichment = new ForkJoin("fork_enrichment", enrichmentForkTasks);
        workflow.add(forkEnrichment);
        // -------------  Enrichment level ended ----------------


        // -------------- Translation Level started ----------------
        com.netflix.conductor.sdk.workflow.def.tasks.Task[][] translationForkTasks = new com.netflix.conductor.sdk.workflow.def.tasks.Task[mediationRules.getTranslations().size()][1];
        for (int i = 0; i < mediationRules.getTranslations().size(); i++) {
            SubWorkflow forkSubworkflow = new SubWorkflow(mediationRules.getTranslations().get(i).getName() + "_subworkflow_ref", "OP_" + mediationRules.getTranslations().get(i).getName(), 1);
            forkSubworkflow.input("sendToORB", mediationRules.getTranslations().get(i).getOrbpFlags());
            for (String enrichmentInput : mediationRules.getTranslations().get(i).getEnrichments()) {
                forkSubworkflow.input(enrichmentInput, "${" + enrichmentInput + "_subworkflow_ref" + ".output.result}");
            }
            translationForkTasks[i][0] = forkSubworkflow;
        }
        ForkJoin forkTranslation = new ForkJoin("fork_translation", translationForkTasks);
        workflow.add(forkTranslation);
        // ------------ Translation Level Ended -------------------


        // -------------- Distribution level started --------------------
        com.netflix.conductor.sdk.workflow.def.tasks.Task[][] distributionForkTasks = new com.netflix.conductor.sdk.workflow.def.tasks.Task[mediationRules.getDistributions().size()][1];
        for (int i = 0; i < mediationRules.getDistributions().size(); i++) {
            SubWorkflow forkSubworkflow = new SubWorkflow(mediationRules.getDistributions().get(i).getDistributeTo() + "_subworkflow_ref", "OP_" + mediationRules.getDistributions().get(i).getDistributeTo(), 1);
            forkSubworkflow.input("sendToORB", mediationRules.getDistributions().get(i).getOrbpFlags());
            forkSubworkflow.input(mediationRules.getDistributions().get(i).getTranslation(), "${" + mediationRules.getDistributions().get(i).getTranslation() + "_subworkflow_ref" + ".output.result}");
            distributionForkTasks[i][0] = forkSubworkflow;
        }
        ForkJoin forkDistribution = new ForkJoin("fork_distribution", distributionForkTasks);
        workflow.add(forkDistribution);
        // ----------- Distribution level Ended --------------------


        WorkflowDef workflowDef = workflow.toWorkflowDef();
        workflowDef.setOutputParameters(Map.of());
        workflowDef.setTimeoutSeconds(0);
        workflowDef.setInputTemplate(Map.of());
        workflowDef.setSchemaVersion(2);
        workflowDef.setInputParameters(List.of());

        return workflowDef;
    }
}
