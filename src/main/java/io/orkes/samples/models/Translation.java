package io.orkes.samples.models;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class Translation {
    private String name;
    private List<String> enrichments;
    private OrbpFlags orbpFlags;
}
