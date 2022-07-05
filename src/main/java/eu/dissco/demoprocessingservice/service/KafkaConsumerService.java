package eu.dissco.demoprocessingservice.service;

import eu.dissco.demoprocessingservice.Profiles;
import io.cloudevents.CloudEvent;
import java.io.IOException;
import java.util.List;
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
public class KafkaConsumerService {

  private final ProcessingService processingService;

  @KafkaListener(topics = "${kafka.consumer.topic}")
  public void getMessages(@Payload List<CloudEvent> messages) throws IOException {
    log.info("Received batch of: {} for kafka", messages.size());
    processingService.handleMessages(messages);
  }

}
