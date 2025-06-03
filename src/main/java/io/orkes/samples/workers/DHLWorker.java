package io.orkes.samples.workers;

import com.netflix.conductor.sdk.workflow.task.WorkerTask;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class DHLWorker {

    @WorkerTask("ship_via_dhl")
    @Tool(description = "Ship a package via DHL")
    public void shipViaDHL() {

    }
}
