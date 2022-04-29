package eu.dissco.demoprocessingservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import eu.dissco.demoprocessingservice.Profiles;
import eu.dissco.demoprocessingservice.exception.AuthenticationException;
import eu.dissco.demoprocessingservice.properties.KafkaConsumerProperties;
import io.cloudevents.CloudEvent;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
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
  private final KafkaPublishService kafkaPublishService;
  private final KafkaConsumerProperties consumerProperties;

  @KafkaListener(topics = "${kafka.consumer.topic}")
  public void getMessages(@Payload List<CloudEvent> messages) throws AuthenticationException {
    log.info("Received batch of: {} for kafka", messages.size());
    var map = messages.stream()
        .map(processingService::processItem)
        .map(CompletableFuture::join)
        .filter(Objects::nonNull)
        .collect(Collectors.toMap(this::getUniqueIdentifier, Function.identity(),
            this::handleDuplicates));
    log.info("Checked batch of: {} Items to upset: {}", map.size(), map.size());
    if (map.isEmpty()) {
      log.info("No records left after processing");
    } else {
      cordraSendService.commitUpsertObject(map.values());
    }
  }

  private JsonNode handleDuplicates(JsonNode existing, JsonNode replacement) {
    log.warn("The same object is present twice in one batch, publishing duplicate back to queue");
    kafkaPublishService.sendMessage(replacement.get("content"), consumerProperties.getTopic());
    return existing;
  }

  private String getUniqueIdentifier(JsonNode object) {
    var authoritative = object.get("content").get("ods:authoritative");
    return authoritative.get("ods:institution").asText() + authoritative.get(
        "ods:physicalSpecimenId").asText();
  }

}
