package eu.dissco.demoprocessingservice.service;

import static eu.dissco.demoprocessingservice.util.TestUtils.createCloudEvent;
import static eu.dissco.demoprocessingservice.util.TestUtils.loadResourceFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.demoprocessingservice.exception.AuthenticationException;
import eu.dissco.demoprocessingservice.properties.KafkaConsumerProperties;
import io.cloudevents.CloudEvent;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import kafka.utils.Json;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KafkaServiceTest {

  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

  @Mock
  private ProcessingService processingService;
  @Mock
  private CordraSendService cordraSendService;
  @Mock
  private KafkaPublishService kafkaPublishService;
  @Mock
  private KafkaConsumerProperties consumerProperties;
  @Captor
  private ArgumentCaptor<Collection<JsonNode>> argumentCaptor;

  private KafkaService service;

  @BeforeEach
  void setup() {
    this.service = new KafkaService(processingService, cordraSendService, kafkaPublishService,
        consumerProperties);
  }

  @Test
  void testGetMessages() throws IOException, AuthenticationException {
    // Given
    var message = loadResourceFile("test-object.json");
    var json = mapper.readTree(loadResourceFile("test-object-full.json"));
    given(processingService.processItem(any())).willReturn(
        CompletableFuture.completedFuture(json));

    // When
    service.getMessages(List.of(createCloudEvent(message)));

    // Then
    then(cordraSendService).should().commitUpsertObject(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue()).hasSize(1);
  }

  @Test
  void testNoMessages() throws IOException, AuthenticationException {
    // Given
    var message = loadResourceFile("test-object.json");
    given(processingService.processItem(any(CloudEvent.class))).willReturn(
        CompletableFuture.completedFuture(null));

    // When
    service.getMessages(List.of(createCloudEvent(message)));

    // Then
    then(cordraSendService).shouldHaveNoInteractions();
  }

  @Test
  void testDuplicatesInBatch() throws IOException, AuthenticationException {
    // Given
    var message = loadResourceFile("test-object.json");
    var messageDifferent = loadResourceFile("test-object-different.json");
    var json = mapper.readTree(loadResourceFile("test-object-full.json"));
    var jsonDifferent = mapper.readTree(loadResourceFile("test-object-different-full.json"));
    given(processingService.processItem(any())).willReturn(
        CompletableFuture.completedFuture(json)).willReturn(CompletableFuture.completedFuture(jsonDifferent));
    given(consumerProperties.getTopic()).willReturn("topic");

    // When
    service.getMessages(List.of(createCloudEvent(message), createCloudEvent(messageDifferent)));

    // Then
    then(cordraSendService).should().commitUpsertObject(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue()).hasSize(1);
    then(kafkaPublishService).should().sendMessage(eq(jsonDifferent.get("content")), eq("topic"));
  }

}
