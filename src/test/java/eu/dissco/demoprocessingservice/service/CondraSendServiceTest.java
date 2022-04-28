package eu.dissco.demoprocessingservice.service;

import static eu.dissco.demoprocessingservice.util.TestUtils.loadResourceFile;
import static eu.dissco.demoprocessingservice.util.TestUtils.loadResourceFileToString;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.dissco.demoprocessingservice.client.CordraFeign;
import eu.dissco.demoprocessingservice.exception.AuthenticationException;
import eu.dissco.demoprocessingservice.properties.CordraProperties;
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

  private CordraSendService cordraSendService;

  @BeforeEach
  void setup() {
    cordraSendService = new CordraSendService(mapper, properties, cordraFeign);
  }

  @Test
  void testSendMessages() throws IOException, AuthenticationException {
    // Given
    var json = givenJson("test-object.json");
    given(cordraFeign.authenticate(any())).willReturn(
        loadResourceFileToString("auth-response.json"));
    given(cordraFeign.postCordraObjects(any(), anyString())).willReturn(
        loadResourceFileToString("upsert-response.json"));
    var result = getJsonObject(json);

    // When
    cordraSendService.commitUpsertObject(List.of(json));

    // Then
    then(cordraFeign).should().postCordraObjects(eq(result), eq("Bearer Test-Token"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"auth-failed-response.json", "auth-invalid-response.json"})
  void testAuthFailed(String filename) throws IOException, AuthenticationException {
    // Given
    var json = givenJson("test-object.json");
    given(cordraFeign.authenticate(any())).willReturn(
        loadResourceFileToString(filename));

    // When
    assertThatThrownBy(() -> cordraSendService.commitUpsertObject(List.of(json))).isInstanceOf(
        AuthenticationException.class);

    // Then
    then(cordraFeign).shouldHaveNoMoreInteractions();
  }

  private JsonNode givenJson(String fileName) throws IOException {
    var objectString = loadResourceFile(fileName);
    var contentNode = (ObjectNode) mapper.readTree(objectString);
    var object = mapper.createObjectNode();
    contentNode.put("@type", properties.getType());
    object.put("type", properties.getType());
    object.set("content", contentNode);
    return object;
  }

  private ArrayNode getJsonObject(JsonNode object) {
    var array = mapper.createArrayNode();
    array.add(object);
    return array;
  }

}
