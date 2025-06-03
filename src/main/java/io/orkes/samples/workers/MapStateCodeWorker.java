package io.orkes.samples.workers;

import com.netflix.conductor.sdk.workflow.task.WorkerTask;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import lombok.Data;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class MapStateCodeWorker {

    @Data
    public static class StateCodeMappingInput {
        private List<Map<String, Object>> statePopulations;
    }

    @WorkerTask("map_state_codes")
    @Tool(description = "Maps state names to their state codes")
    public Map<String, Object> mapStateCodes(
            @ToolParam(description = "Input containing state populations data") StateCodeMappingInput input) {

        Map<String, Object> result = new HashMap<>();

        List<Map<String, Object>> statePopulations = input.getStatePopulations();

        statePopulations.forEach(stateData -> {
            String state = (String) stateData.get("State");
            stateData.put("stateCode", Constants.STATE_CODES.getOrDefault(state, "NOT_AVAILABLE"));
        });

        String currentTimeOnServer = Instant.now().toString();
        // Include log information in the output
        result.put("log", "Mapped states to codes at " + currentTimeOnServer);
        result.put("statePopulations", statePopulations);

        return result;
    }
}