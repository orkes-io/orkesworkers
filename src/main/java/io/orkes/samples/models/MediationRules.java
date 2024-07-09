package io.orkes.samples.models;

import lombok.Data;

import java.util.List;

@Data
public class MediationRules {
    private List<Enrichment> enrichments;
    private List<Translation> translations;
    private List<Distribution> distributions;
}
