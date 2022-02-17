package eu.dissco.demoprocessingservice.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

import java.util.List;

@Data
public class OpenDSWrapper {

    @JsonProperty("ods:authoritative")
    Authoritative authoritative;
    @JsonProperty("ods:images")
    List<Image> images;
    String sourceId;

}
