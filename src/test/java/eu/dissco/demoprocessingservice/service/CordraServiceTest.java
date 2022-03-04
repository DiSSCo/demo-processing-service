package eu.dissco.demoprocessingservice.service;

import static eu.dissco.demoprocessingservice.util.TestUtils.loadResourceFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import eu.dissco.demoprocessingservice.client.CordraFeign;
import eu.dissco.demoprocessingservice.domain.OpenDSWrapper;
import eu.dissco.demoprocessingservice.properties.CordraProperties;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CordraServiceTest {

  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

  @Mock
  private ValidationService validationService;
  @Mock
  private CordraProperties properties;
  @Mock
  private CordraFeign cordraFeign;
  @Mock
  private KafkaPublishService kafkaPublishService;
  private CordraService service;
  private JsonSchema schema;

  @BeforeEach
  void setup() throws IOException {
    this.service = new CordraService(cordraFeign, mapper, validationService, properties, kafkaPublishService);
    var factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
    this.schema = factory.getSchema(loadResourceFile("schema.json"));
  }

  @Test
  void testNewObject() throws Exception {
    // Given
    var message = loadResourceFile("test-object.json");
    given(cordraFeign.search(anyString())).willReturn(loadResourceFile("test-empty-search.json"));
    given(validationService.retrieveSchema(anyString())).willReturn(schema);
    given(properties.getType()).willReturn("ODStypeV0.2-Test");

    // When
    var result = service.processItem(message);

    // Then
    var messageNode = (ObjectNode) mapper.readTree(message);
    messageNode.put("@type", "ODStypeV0.2-Test");
    assertThat(result.get()).isEqualTo(mapper.treeToValue(messageNode, OpenDSWrapper.class));
  }

  @Test
  void testEqualObject() throws Exception {
    // Given
    var message = loadResourceFile("test-object.json");
    given(cordraFeign.search(anyString())).willReturn(loadResourceFile("test-search-equal.json"));
    given(properties.getType()).willReturn("ODStypeV0.2-Test");

    // When
    var result = service.processItem(message);

    // Then
    then(cordraFeign).shouldHaveNoMoreInteractions();
    assertThat(result.get()).isNull();
  }

  @Test
  void testUnequalObject() throws Exception {
    // Given
    var message = loadResourceFile("test-object.json");
    given(cordraFeign.search(anyString())).willReturn(loadResourceFile("test-search-unequal.json"));
    given(validationService.retrieveSchema(anyString())).willReturn(schema);
    given(properties.getType()).willReturn("ODStypeV0.2-Test");

    // When
    var result = service.processItem(message);

    // Then
    var messageNode = (ObjectNode) mapper.readTree(message);
    messageNode.put("@type", "ODStypeV0.2-Test");
    var expected = mapper.treeToValue(messageNode, OpenDSWrapper.class);
    expected.setId("test/eab36efab0bf0e60dfe0");
    then(cordraFeign).shouldHaveNoMoreInteractions();
    assertThat(result.get()).isEqualTo(expected);
  }

  @Test
  void testInvalidJson() throws Exception {
    // Given
    var message = loadResourceFile("test-object-invalid.json");

    // When
    var result = service.processItem(message);

    // Then
    then(cordraFeign).shouldHaveNoInteractions();
    assertThat(result.get()).isNull();
  }


  @Test
  void testValidationError() throws Exception {
    // Given
    var message = loadResourceFile("test-object-invalid-schema.json");
    given(cordraFeign.search(anyString())).willReturn(loadResourceFile("test-empty-search.json"));
    var factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
    var schema = factory.getSchema(loadResourceFile("schema.json"));
    given(validationService.retrieveSchema(anyString()))
        .willReturn(schema);
    given(properties.getType()).willReturn("ODStypeV0.2-Test");

    // When
    service.processItem(message);

    // Then
    then(cordraFeign).shouldHaveNoMoreInteractions();
  }

}
