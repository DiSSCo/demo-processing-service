package eu.dissco.demoprocessingservice.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Image {

  @JsonProperty("ods:imageURI")
  String imageUri;
}
