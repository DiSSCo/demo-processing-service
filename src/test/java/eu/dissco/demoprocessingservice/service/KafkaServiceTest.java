package eu.dissco.demoprocessingservice.service;

import static eu.dissco.demoprocessingservice.util.TestUtils.loadResourceFile;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.demoprocessingservice.domain.OpenDSWrapper;
import eu.dissco.demoprocessingservice.repository.CordraRepository;
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
  private CordraRepository cordraSendService;

  private KafkaService service;

  @BeforeEach
  void setup() {
    this.service = new KafkaService(cordraService, cordraSendService);
  }

  @Test
  void testGetMessages() throws IOException {
    // Given
    var message = loadResourceFile("test-object.json");
    var openDS = mapper.readValue(message,
        OpenDSWrapper.class);
    given(cordraService.processItem(anyString())).willReturn(
        CompletableFuture.completedFuture(openDS));

    // When
    service.getMessages(List.of(message));

    // Then
    then(cordraSendService).should().saveItems(eq(List.of(openDS)));
  }

  @Test
  void testNoMessages() throws IOException {
    // Given
    var message = loadResourceFile("test-object.json");
    var openDS = mapper.readValue(message,
        OpenDSWrapper.class);
    given(cordraService.processItem(anyString())).willReturn(
        CompletableFuture.completedFuture(null));

    // When
    service.getMessages(List.of(message));

    // Then
    then(cordraSendService).shouldHaveNoInteractions();
  }


}
