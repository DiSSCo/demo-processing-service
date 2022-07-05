package eu.dissco.demoprocessingservice.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@JsonInclude(Include.NON_NULL)
public class OpenDSWrapper {

  @JsonProperty("ods:authoritative")
  private Authoritative authoritative;
  @JsonProperty("ods:images")
  private List<Image> images;
  @JsonProperty("ods:unmapped")
  private JsonNode unmapped;
  @JsonProperty("@type")
  @EqualsAndHashCode.Exclude
  private String type;
  @JsonProperty("@id")
  @EqualsAndHashCode.Exclude
  private String id;

}
