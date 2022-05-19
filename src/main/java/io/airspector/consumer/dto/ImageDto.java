package io.airspector.consumer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImageDto {

    @JsonProperty(value = "path")
    private String path;

    @JsonProperty(value = "fileName")
    private String fileName;

    @JsonProperty(value = "pitch")
    private Double pitch;

    @JsonProperty(value = "height")
    private Double height;
}
