package eu.dissco.demoprocessingservice.configuration;

import eu.dissco.demoprocessingservice.properties.CordraProperties;
import lombok.AllArgsConstructor;
import net.cnri.cordra.api.CordraClient;
import net.cnri.cordra.api.CordraException;
import net.cnri.cordra.api.TokenUsingHttpCordraClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AllArgsConstructor
public class CordraConfiguration {

  private final CordraProperties properties;

  @Bean
  CordraClient cordraClient() throws CordraException {
    return new TokenUsingHttpCordraClient(properties.getHost(), properties.getUsername(),
        properties.getPassword());
  }

}
