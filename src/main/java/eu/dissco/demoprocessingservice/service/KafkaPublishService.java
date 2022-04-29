package eu.dissco.demoprocessingservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.demoprocessingservice.domain.OpenDSWrapper;
import eu.dissco.demoprocessingservice.properties.ApplicationProperties;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

@Service
@Slf4j
@AllArgsConstructor
public class KafkaPublishService {

  private final ObjectMapper mapper;
  private final KafkaTemplate<String, CloudEvent> kafkaTemplate;
  private final ApplicationProperties applicationProperties;

  public void sendMessage(OpenDSWrapper openDSWrapper, String topic) {
    try {
      var data = mapper.writeValueAsBytes(openDSWrapper);
      var event = createCloudEvent(data);
      publishMessage(event, topic);
    } catch (JsonProcessingException e) {
      log.error("Unable to deserialize the object: {}", openDSWrapper);
    }
  }

  public void sendMessage(JsonNode node, String topic) {
    try {
      var eventMessage = mapper.createObjectNode();
      eventMessage.set("openDS", node);
      var data = mapper.writeValueAsBytes(eventMessage);
      var event = createCloudEvent(data);
      publishMessage(event, topic);
    } catch (JsonProcessingException e) {
      log.error("Unable to deserialize the object: {}", node);
    }
  }

  private void publishMessage(CloudEvent event, String topic) {
    ListenableFuture<SendResult<String, CloudEvent>> future = kafkaTemplate.send(topic, event);
    future.addCallback(new ListenableFutureCallback<>() {

      @Override
      public void onSuccess(SendResult<String, CloudEvent> result) {
        log.debug("Message successfully send to: {}", topic);
      }

      @Override
      public void onFailure(Throwable ex) {
        log.error("Unable to send message: {}", event, ex);
      }
    });
  }

  private CloudEvent createCloudEvent(byte[] data) {
    return CloudEventBuilder.v1()
        .withId(UUID.randomUUID().toString())
        .withType(applicationProperties.getEventType())
        .withSource(URI.create(applicationProperties.getEndpoint()))
        .withSubject(applicationProperties.getServiceName())
        .withTime(OffsetDateTime.now(ZoneOffset.UTC))
        .withDataContentType("application/json")
        .withData(data)
        .build();
  }
}
