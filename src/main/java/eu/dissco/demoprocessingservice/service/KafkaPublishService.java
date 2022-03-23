package eu.dissco.demoprocessingservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.demoprocessingservice.domain.OpenDSWrapper;
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
  private final KafkaTemplate<String, String> kafkaTemplate;

  public void sendMessage(OpenDSWrapper openDSWrapper, String topic) {
    try {
      var json = mapper.writeValueAsString(openDSWrapper);
      ListenableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, json);
      future.addCallback(new ListenableFutureCallback<>() {

        @Override
        public void onSuccess(SendResult<String, String> result) {
          log.debug("Message successfully send to: {}", topic);
        }

        @Override
        public void onFailure(Throwable ex) {
          log.error("Unable to send message: {}", json, ex);
        }
      });
    } catch (JsonProcessingException e) {
      log.error("Failed to pars Objects to Json: {}", openDSWrapper);
    }
  }
}
