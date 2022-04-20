package eu.dissco.demoprocessingservice.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
@JsonInclude(Include.NON_NULL)
public class EventData {

  @JsonProperty("openDS")
  OpenDSWrapper openDS;

  @JsonProperty("enrichment")
  List<Enrichment> enrichment;

}
