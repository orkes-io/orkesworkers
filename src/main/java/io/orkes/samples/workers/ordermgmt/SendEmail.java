package io.orkes.samples.workers.ordermgmt;

import java.util.HashMap;
import java.util.Map;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.netflix.conductor.sdk.workflow.task.WorkerTask;

import lombok.Data;

@Component
public class SendEmail {

  @Data
  public static class SendEmailInput {}

  @WorkerTask("send_email")
  @Tool(description = "Sends email notifications")
  public Map<String, Object> executeSendEmail(
      @ToolParam(description = "Input parameters for email sending") SendEmailInput input) {

    return new HashMap<>();
  }
}
