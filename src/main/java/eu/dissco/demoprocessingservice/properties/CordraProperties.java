package eu.dissco.demoprocessingservice.properties;

import javax.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties("cordra")
public class CordraProperties {

  @NotBlank
  private String host;

  @NotBlank
  private String username;

  @NotBlank
  private String password;

}
