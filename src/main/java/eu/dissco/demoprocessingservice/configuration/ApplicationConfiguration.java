package eu.dissco.demoprocessingservice.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ApplicationConfiguration {

  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper().findAndRegisterModules();
  }

  @Bean(name = "processingThreadPoolTaskExecutor")
  public Executor processingThreadPoolTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(8);
    executor.setMaxPoolSize(100);
    executor.setQueueCapacity(5500);
    executor.setPrestartAllCoreThreads(true);
    executor.setThreadNamePrefix("ProcessingService-");
    executor.afterPropertiesSet();
    return executor;
  }

}
