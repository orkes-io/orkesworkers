package io.orkes.samples.models;

import lombok.Data;

import java.util.Map;

@Data
public class Distribution {
    private String translation;
    private String distributeTo;
    private String sendToORB = "Y";
    private String taskType = "HTTP";
    private String natsWorkflowName;
    private Integer natsWorkflowVersion;
    private Map<String, Object> natsWorkflowInput;
}
