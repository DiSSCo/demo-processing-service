package eu.dissco.demoprocessingservice.service;

import eu.dissco.demoprocessingservice.Profiles;
import eu.dissco.demoprocessingservice.exception.AuthenticationException;
import io.cloudevents.CloudEvent;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Profile(Profiles.KAFKA)
@AllArgsConstructor
public class KafkaService {

  private final ProcessingService processingService;
  private final CordraSendService cordraSendService;

  @KafkaListener(topics = "${kafka.topic}")
  public void getMessages(@Payload List<CloudEvent> messages) throws AuthenticationException {
    log.info("Received batch of: {} for kafka", messages.size());
    var list = messages.stream()
        .map(processingService::processItem)
        .map(CompletableFuture::join)
        .filter(Objects::nonNull).toList();
    log.info("Checked batch of: {} Items to upset: {}", list.size(), list.size());
    if (list.isEmpty()) {
      log.info("No records left after processing");
    } else {
      cordraSendService.commitUpsertObject(list);
    }
  }

}
