package io.orkes.samples.workers;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ThumbnailInfo {
    String fileLocation;
    Long pts;
    Double pts_time;
}