package eu.dissco.demoprocessingservice.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class KafkaService {

  private final CordraService cordraService;

  @KafkaListener(topics = "${kafka.topic}")
  public void getMessages(@Payload String message, @Header(KafkaHeaders.OFFSET) int offset) {
    cordraService.processItem(message, offset);
  }

}
