package eu.dissco.demoprocessingservice.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import lombok.Data;

@Data
public class OpenDSWrapper {

  @JsonProperty("ods:authoritative")
  private Authoritative authoritative;
  @JsonProperty("ods:images")
  private List<Image> images;
  @JsonProperty("sourceId")
  private String sourceId;
  @JsonProperty("ods:unmapped")
  private JsonNode unmapped;
  @JsonProperty("@type")
  private String type;
  @JsonIgnore
  private String id;

}
