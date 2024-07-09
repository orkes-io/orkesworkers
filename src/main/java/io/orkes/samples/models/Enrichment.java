package io.orkes.samples.models;

import lombok.Data;

import java.util.Map;

@Data
public class Enrichment {
    private String enrichmentType;
    private String sendToORB = "N";
    private String taskType = "HTTP";
    private String natsWorkflowName;
    private Integer natsWorkflowVersion;
    private Map<String, Object> natsWorkflowInput;
}
