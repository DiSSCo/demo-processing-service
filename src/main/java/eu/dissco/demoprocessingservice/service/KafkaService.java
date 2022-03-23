package eu.dissco.demoprocessingservice.service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class KafkaService {

  private final CordraService cordraService;
  private final CordraSendService cordraSendService;

  @KafkaListener(topics = "${kafka.topic}")
  public void getMessages(@Payload List<String> messages) {
    log.info("Received batch of: {} for kafka", messages.size());
    var list = messages.stream()
        .map(cordraService::processItem)
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
