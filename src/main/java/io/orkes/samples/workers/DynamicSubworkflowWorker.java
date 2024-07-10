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
import io.orkes.samples.models.MediationRules;
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
        return "quest_start_subworkflow";
    }

    /**
     * This Worker will start 'dynamic_workflow' workflow and pass the subworkflow definitions using createDynamicSubworkflow() method
     * @param task
     * @return
     */
    @Override
    public TaskResult execute(Task task) {
        System.out.println("Starting quest_start_subworkflow task");
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
        StartWorkflowRequest request = new StartWorkflowRequest();
        request.setName("dynamic_workflow");
        Map<String, Object> inputData = new HashMap<>();
        //inputData.put("enrichmentSubworkflowsDef", subworkflowDef());
        Object dynamicSubworkflowDef = objectMapper.convertValue(createDynamicSubworkflow(mediationRules), Object.class);
        inputData.put("dynamicSubworkflowDef", dynamicSubworkflowDef);
        request.setInput(inputData);

        String workflowId = workflowClient.startWorkflow(request);
        log.info("Workflow id: {}", workflowId);
        return Map.of("workflowId", workflowId);
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
            ConductorWorkflow conductorWorkflow = new ConductorWorkflow(executor);
            conductorWorkflow.setName(mediationRules.getEnrichments().get(i).getEnrichmentType() + "_workflow");

            SubWorkflow natsSubworkflow = new SubWorkflow("nats_" + mediationRules.getEnrichments().get(i).getEnrichmentType() + "_subworkflow_ref", mediationRules.getEnrichments().get(i).getNatsWorkflowName(), mediationRules.getEnrichments().get(i).getNatsWorkflowVersion());
            natsSubworkflow.input(mediationRules.getEnrichments().get(i).getNatsWorkflowInput());
            Switch sendToORBEnrichmentSwitch = new Switch("send_to_" + mediationRules.getEnrichments().get(i).getEnrichmentType() + "_switch", "${workflow.input.sendToORB}").switchCase("Y", natsSubworkflow).defaultCase(List.of());
            conductorWorkflow.add(sendToORBEnrichmentSwitch);

            Http httptask = new Http(mediationRules.getEnrichments().get(i).getEnrichmentType() + "_enrichment_workflow_task");
            httptask.url("https://orkes-api-tester.orkesconductor.com/api");
            conductorWorkflow.add(httptask);

            SubWorkflow forkSubworkflow = new SubWorkflow(mediationRules.getEnrichments().get(i).getEnrichmentType() + "_subworkflow_ref", conductorWorkflow);
            forkSubworkflow.input("sendToORB", mediationRules.getEnrichments().get(i).getSendToORB());
            enrichmentForkTasks[i][0] = forkSubworkflow;
        }
        ForkJoin forkEnrichment = new ForkJoin("fork_enrichment", enrichmentForkTasks);
        workflow.add(forkEnrichment);
        // -------------  Enrichment level ended ----------------


        // -------------- Translation Level started ----------------
        com.netflix.conductor.sdk.workflow.def.tasks.Task[][] translationForkTasks = new com.netflix.conductor.sdk.workflow.def.tasks.Task[mediationRules.getTranslations().size()][1];
        for (int i = 0; i < mediationRules.getTranslations().size(); i++) {
            ConductorWorkflow conductorWorkflow = new ConductorWorkflow(executor);
            SubWorkflow forkSubworkflow = new SubWorkflow(mediationRules.getTranslations().get(i).getName() + "_subworkflow_ref", conductorWorkflow);
            forkSubworkflow.input("sendToORB", mediationRules.getTranslations().get(i).getSendToORB());
            conductorWorkflow.setName(mediationRules.getTranslations().get(i).getName() + "_workflow");
            SubWorkflow natsSubworkflow = new SubWorkflow("nats_" + mediationRules.getTranslations().get(i).getName() + "_subworkflow_ref", mediationRules.getTranslations().get(i).getNatsWorkflowName(), mediationRules.getTranslations().get(i).getNatsWorkflowVersion());
            natsSubworkflow.input(mediationRules.getTranslations().get(i).getNatsWorkflowInput());
            Switch sendToORBTranslationSwitch = new Switch("send_to_" + mediationRules.getTranslations().get(i).getName() + "_switch", "${workflow.input.sendToORB}").switchCase("Y", natsSubworkflow).defaultCase(List.of());
            conductorWorkflow.add(sendToORBTranslationSwitch);

            for (int j = 0; j < mediationRules.getTranslations().get(i).getEnrichments().size(); j++) {
                Http httptask = new Http(mediationRules.getTranslations().get(i).getEnrichments().get(j)+ "_translations_workflow_task");
                httptask.url("https://orkes-api-tester.orkesconductor.com/api");
                String taskRef = mediationRules.getTranslations().get(i).getEnrichments().get(j)  + "_subworkflow_ref";
                String outputExpression = "${" + taskRef + ".output.response}"; //Can differ with different different tasks. Example with Simple/Inline tasks we might have to use result
                forkSubworkflow.input(mediationRules.getTranslations().get(i).getEnrichments().get(j), outputExpression);
                conductorWorkflow.add(httptask);
            }

            translationForkTasks[i][0] = forkSubworkflow;
        }
        ForkJoin forkTranslation = new ForkJoin("fork_translation", translationForkTasks);
        workflow.add(forkTranslation);
        // ------------ Translation Level Ended -------------------


        // -------------- Distribution level started --------------------
        com.netflix.conductor.sdk.workflow.def.tasks.Task[][] distributionForkTasks = new com.netflix.conductor.sdk.workflow.def.tasks.Task[mediationRules.getDistributions().size()][1];
        for (int i = 0; i < mediationRules.getDistributions().size(); i++) {
            ConductorWorkflow conductorWorkflow = new ConductorWorkflow(executor);
            conductorWorkflow.setName(mediationRules.getDistributions().get(i).getDistributeTo() + "_workflow");

            SubWorkflow natsSubworkflow = new SubWorkflow("nats_" + mediationRules.getDistributions().get(i).getDistributeTo() + "_subworkflow_ref", mediationRules.getDistributions().get(i).getNatsWorkflowName(), mediationRules.getDistributions().get(i).getNatsWorkflowVersion());
            natsSubworkflow.input(mediationRules.getDistributions().get(i).getNatsWorkflowInput());
            Switch sendToORBDistributionSwitch = new Switch("send_to_" + mediationRules.getDistributions().get(i).getDistributeTo() + "_switch", "${workflow.input.sendToORB}").switchCase("Y", natsSubworkflow).defaultCase(List.of());
            conductorWorkflow.add(sendToORBDistributionSwitch);

            Http httptask = new Http(mediationRules.getDistributions().get(i).getDistributeTo() + "_distributions_workflow_task");
            httptask.url("https://orkes-api-tester.orkesconductor.com/api");
            conductorWorkflow.add(httptask);

            SubWorkflow forkSubworkflow = new SubWorkflow(mediationRules.getDistributions().get(i).getDistributeTo() + "_subworkflow_ref", conductorWorkflow);
            forkSubworkflow.input("sendToORB", mediationRules.getDistributions().get(i).getSendToORB());
            String taskRef = mediationRules.getDistributions().get(i).getTranslation()  + "_subworkflow_ref";
            String outputExpression = "${" + taskRef + ".output.response}"; //Can differ with different different tasks. Example with Simple/Inline tasks we might have to use result
            forkSubworkflow.input(mediationRules.getDistributions().get(i).getTranslation(), outputExpression);
            forkSubworkflow.input("sink", "nats:nats-integ:subject");
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
