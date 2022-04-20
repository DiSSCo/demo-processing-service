package eu.dissco.demoprocessingservice.domain;

import lombok.Builder;
import lombok.Data;

@Data
public class Enrichment {

  private String name;
  private boolean imageOnly;
}
