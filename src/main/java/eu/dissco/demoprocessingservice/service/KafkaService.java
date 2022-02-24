package eu.dissco.demoprocessingservice.service;

import eu.dissco.demoprocessingservice.domain.OpenDSWrapper;
import java.util.ArrayList;
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
    var futures = new ArrayList<CompletableFuture<OpenDSWrapper>>();
    messages.forEach(message -> futures.add(cordraService.processItem(message)));
    var results = futures.stream().map(CompletableFuture::join).filter(Objects::nonNull).toList();
    cordraSendService.commitUpsertObject(results);
  }

}
