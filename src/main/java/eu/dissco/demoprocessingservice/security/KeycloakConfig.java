package eu.dissco.demoprocessingservice.security;

import eu.dissco.demoprocessingservice.Profiles;
import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile(Profiles.WEB)
@Configuration
public class KeycloakConfig {

  @Bean
  public KeycloakSpringBootConfigResolver keycloakConfigResolver(){
    return new KeycloakSpringBootConfigResolver();
  }
}
