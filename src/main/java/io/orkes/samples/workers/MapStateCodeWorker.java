package io.orkes.samples.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.springframework.boot.json.JsonParseException;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
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
        List<Map<String, Object>> data = (List<Map<String, Object>>) task.getInputData().get("data");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        String currentTimeOnServer = Instant.now().toString();
        result.log("Mapped states to codes at " + currentTimeOnServer);
        result.addOutputData("statePopulations", data);
        return result;
    }

}
