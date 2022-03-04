package eu.dissco.demoprocessingservice.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import lombok.Data;

@Data
public class Image {

  @JsonProperty("ods:imageURI")
  String imageUri;
  @JsonProperty("additional_info")
  List<JsonNode> additionalInfo;
}
