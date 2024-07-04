package io.orkes.samples.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import io.orkes.conductor.client.http.OrkesIntegrationClient;
import io.orkes.conductor.client.model.integration.Integration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class IntegrationsWorker implements Worker {

    @Autowired
    private OrkesIntegrationClient orkesIntegrationClient;

    @Override
    public String getTaskDefName() {
        return "integrations_worker";
    }

    @Override
    public TaskResult execute(Task task) {

        TaskResult result = new TaskResult(task);
        log.info("Worker integrations_worker is being started");
        result.setStatus(TaskResult.Status.COMPLETED);
        Integration integration = orkesIntegrationClient.getIntegration("anthropic_saas");
        System.out.println("Integration is : " + integration);
        return result;
    }
}
