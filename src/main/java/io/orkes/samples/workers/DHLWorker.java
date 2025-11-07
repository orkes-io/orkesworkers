package io.orkes.samples.workers;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import com.netflix.conductor.sdk.workflow.task.WorkerTask;

@Component
public class DHLWorker {

  @WorkerTask("ship_via_dhl")
  @Tool(description = "Ship a package via DHL")
  public void shipViaDHL() {}
}
