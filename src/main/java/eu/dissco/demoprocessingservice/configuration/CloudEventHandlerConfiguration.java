package eu.dissco.demoprocessingservice.configuration;

import eu.dissco.demoprocessingservice.Profiles;
import io.cloudevents.spring.mvc.CloudEventHttpMessageConverter;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Profile(Profiles.WEB)
@Configuration
public class CloudEventHandlerConfiguration implements WebMvcConfigurer {

  @Override
  public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
    converters.add(0, new CloudEventHttpMessageConverter());
  }

}


