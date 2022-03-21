package eu.dissco.demoprocessingservice.service;

import static eu.dissco.demoprocessingservice.util.TestUtils.loadResourceFile;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.dissco.demoprocessingservice.client.CordraFeign;
import eu.dissco.demoprocessingservice.domain.OpenDSWrapper;
import eu.dissco.demoprocessingservice.properties.CordraProperties;
import eu.dissco.demoprocessingservice.repository.CordraRepository;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CondraSendServiceTest {

  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

  @Mock
  private CordraFeign cordraFeign;
  @Mock
  private CordraProperties properties;

  private CordraRepository cordraSendService;

  @BeforeEach
  void setup() {
    cordraSendService = new CordraRepository(mapper, properties, cordraFeign);
  }

  @Test
  void testSendMessages() throws IOException {
    // Given
    var objectString = loadResourceFile("test-object.json");
    var object = mapper.readValue(objectString, OpenDSWrapper.class);
    given(cordraFeign.authenticate(any())).willReturn(loadResourceFile("auth-response.json"));
    given(cordraFeign.postCordraObjects(any(), anyString())).willReturn(
        loadResourceFile("upsert-response.json"));
    given(properties.getType()).willReturn("OpenDSType");
    var result = getJsonObject(object);

    // When
    cordraSendService.saveItems(List.of(object));

    // Then
    then(cordraFeign).should().postCordraObjects(eq(result), eq("Bearer Test-Token"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"auth-failed-response.json", "auth-invalid-response.json"})
  void testAuthFailed(String filename) throws IOException {
    // Given
    var objectString = loadResourceFile("test-object.json");
    var object = mapper.readValue(objectString, OpenDSWrapper.class);
    given(cordraFeign.authenticate(any())).willReturn(
        loadResourceFile(filename));

    // When
    cordraSendService.saveItems(List.of(object));

    // Then
    then(cordraFeign).shouldHaveNoMoreInteractions();
  }

  private ArrayNode getJsonObject(OpenDSWrapper objectString) {
    var array = mapper.createArrayNode();
    var object = mapper.createObjectNode();
    var contentNode = (ObjectNode) mapper.valueToTree(objectString);
    contentNode.put("@type", properties.getType());
    object.put("type", properties.getType());
    object.set("content", contentNode);
    array.add(object);
    return array;
  }

}
