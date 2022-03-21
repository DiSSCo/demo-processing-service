package eu.dissco.demoprocessingservice.configuration;

import eu.dissco.demoprocessingservice.Profiles;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.AsyncConfigurer;

@Configuration
@Profile(Profiles.SERVICE)
public class SpringAsyncConfig implements AsyncConfigurer {

  @Override
  public Executor getAsyncExecutor() {
    return Executors.newFixedThreadPool(40);
  }

}
