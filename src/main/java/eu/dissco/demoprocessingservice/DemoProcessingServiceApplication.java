package eu.dissco.demoprocessingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableKafka
@EnableAsync
public class DemoProcessingServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(DemoProcessingServiceApplication.class, args);
  }

}
