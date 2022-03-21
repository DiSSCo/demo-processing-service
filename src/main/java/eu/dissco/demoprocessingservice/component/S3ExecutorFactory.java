package eu.dissco.demoprocessingservice.component;

import com.amazonaws.client.builder.ExecutorFactory;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.stereotype.Component;

@Component
public class S3ExecutorFactory implements ExecutorFactory {

  @Override
  public ExecutorService newExecutor() {
    return Executors.newFixedThreadPool(40);
  }
}
