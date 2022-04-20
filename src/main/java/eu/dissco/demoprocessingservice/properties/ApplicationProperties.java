package eu.dissco.demoprocessingservice.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties("application")
public class ApplicationProperties {

  private String eventType = "eu.dissco.enrichment.request.event";

  private String serviceName = "processing-service";

  private String endpoint = "https://www.dissco.eu/";

}
