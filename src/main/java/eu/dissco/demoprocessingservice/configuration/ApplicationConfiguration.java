package eu.dissco.demoprocessingservice.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.demoprocessingservice.Profiles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class ApplicationConfiguration {

    @Bean
    public ObjectMapper objectMapper(){
        return new ObjectMapper().findAndRegisterModules();
    }
}
