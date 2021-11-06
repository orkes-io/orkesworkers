package io.orkes.samples.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class MapStateCodeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "map_state_codes";
    }


    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> statePopulations = (List<Map<String, Object>>) task.getInputData().get("statePopulations");

        statePopulations.forEach(stateData -> {
            String state = (String) stateData.get("State");
            stateData.put("stateCode", Constants.STATE_CODES.getOrDefault(state, "NOT_AVAILABLE"));
        });

        TaskResult result = new TaskResult(task);
        String currentTimeOnServer = Instant.now().toString();
        result.log("Mapped states to codes at " + currentTimeOnServer);

        result.addOutputData("statePopulations", statePopulations);
        result.setStatus(TaskResult.Status.COMPLETED);
        return result;
    }

}
