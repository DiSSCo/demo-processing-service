package eu.dissco.demoprocessingservice.configuration;

import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import eu.dissco.demoprocessingservice.Profiles;
import eu.dissco.demoprocessingservice.component.S3ExecutorFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile(Profiles.SERVICE)
public class S3Configuration {

  @Bean
  public TransferManager transferManager() {
    return TransferManagerBuilder.standard().withExecutorFactory(new S3ExecutorFactory()).build();
  }

}
