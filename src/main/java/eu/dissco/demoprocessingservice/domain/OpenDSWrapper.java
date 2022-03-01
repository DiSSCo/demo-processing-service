package eu.dissco.demoprocessingservice.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import lombok.Data;

@Data
public class OpenDSWrapper {

  @JsonProperty("ods:authoritative")
  Authoritative authoritative;
  @JsonProperty("ods:images")
  List<Image> images;
  @JsonProperty("sourceId")
  String sourceId;
  @JsonProperty("ods:unmapped")
  private JsonNode unmapped;
  @JsonProperty("@type")
  String type;

}
