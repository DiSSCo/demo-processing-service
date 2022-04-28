package eu.dissco.demoprocessingservice.service;

import static eu.dissco.demoprocessingservice.util.TestUtils.createCloudEvent;
import static eu.dissco.demoprocessingservice.util.TestUtils.loadResourceFile;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.demoprocessingservice.exception.AuthenticationException;
import io.cloudevents.CloudEvent;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KafkaServiceTest {

  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

  @Mock
  private ProcessingService cordraService;
  @Mock
  private CordraSendService cordraSendService;

  private KafkaService service;

  @BeforeEach
  void setup() {
    this.service = new KafkaService(cordraService, cordraSendService);
  }

  @Test
  void testGetMessages() throws IOException, AuthenticationException {
    // Given
    var message = loadResourceFile("test-object.json");
    var json = mapper.readTree(loadResourceFile("test-object-full.json"));
    given(cordraService.processItem(any())).willReturn(
        CompletableFuture.completedFuture(json));

    // When
    service.getMessages(List.of(createCloudEvent(message)));

    // Then
    then(cordraSendService).should().commitUpsertObject(eq(List.of(json)));
  }

  @Test
  void testNoMessages() throws IOException, AuthenticationException {
    // Given
    var message = loadResourceFile("test-object.json");
    given(cordraService.processItem(any(CloudEvent.class))).willReturn(
        CompletableFuture.completedFuture(null));

    // When
    service.getMessages(List.of(createCloudEvent(message)));

    // Then
    then(cordraSendService).shouldHaveNoInteractions();
  }

}
