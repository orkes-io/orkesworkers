package io.orkes.samples.models;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class Translation {
    private String name;
    private List<String> enrichments;
    private String sendToORB = "N";
    private String taskType = "HTTP";
    private String natsWorkflowName;
    private Integer natsWorkflowVersion;
    private Map<String, Object> natsWorkflowInput;

}
