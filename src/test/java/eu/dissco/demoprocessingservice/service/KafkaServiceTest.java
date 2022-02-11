package eu.dissco.demoprocessingservice.service;

import static eu.dissco.demoprocessingservice.util.TestUtils.loadResourceFile;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;

import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KafkaServiceTest {

  @Mock
  private CordraService cordraService;

  private KafkaService service;

  @BeforeEach
  void setup(){
    this.service = new KafkaService(cordraService);
  }

  @Test
  void testGetMessages() throws IOException {
    // Given
    var message = loadResourceFile("test-object.json");

    // When
    service.getMessages(message, 1);

    // Then
    then(cordraService).should().processItem(eq(message), eq(1));
  }


}
