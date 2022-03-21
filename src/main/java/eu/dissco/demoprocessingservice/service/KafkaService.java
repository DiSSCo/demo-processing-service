package eu.dissco.demoprocessingservice.service;

import eu.dissco.demoprocessingservice.domain.OpenDSWrapper;
import eu.dissco.demoprocessingservice.repository.ProcessingRepository;
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

  private final ProcessingService cordraService;
  private final ProcessingRepository repository;

  @KafkaListener(topics = "${kafka.topic}")
  public void getMessages(@Payload List<String> messages) {
    log.info("Received batch of: {} for kafka", messages.size());
    var futures = new ArrayList<CompletableFuture<OpenDSWrapper>>();
    messages.forEach(message -> futures.add(cordraService.processItem(message)));
    var stream = futures.stream().map(CompletableFuture::join).filter(Objects::nonNull).toList();
    if (stream.isEmpty()){
      log.info("No items need to be updated");
    } else{
      repository.saveItems(stream);
    }
    log.info("Successfully processed message: {}", messages.size());
  }

}
