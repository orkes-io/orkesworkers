package io.orkes.samples.models;

import lombok.Data;

import java.util.Map;

@Data
public class Distribution {
    private String translation;
    private String distributeTo;
    private OrbpFlags orbpFlags;
}
