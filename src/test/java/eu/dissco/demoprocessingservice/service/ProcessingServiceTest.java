package eu.dissco.demoprocessingservice.service;

import static eu.dissco.demoprocessingservice.util.TestUtils.createCloudEvent;
import static eu.dissco.demoprocessingservice.util.TestUtils.loadResourceFile;
import static eu.dissco.demoprocessingservice.util.TestUtils.loadResourceFileToString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import eu.dissco.demoprocessingservice.client.CordraFeign;
import eu.dissco.demoprocessingservice.domain.OpenDSWrapper;
import eu.dissco.demoprocessingservice.exception.JsonValidationException;
import eu.dissco.demoprocessingservice.properties.CordraProperties;
import eu.dissco.demoprocessingservice.util.TestUtils;
import io.cloudevents.CloudEvent;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessingServiceTest {

  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

  @Mock
  private ValidationService validationService;
  @Mock
  private CordraProperties properties;
  @Mock
  private CordraFeign cordraFeign;
  @Mock
  private KafkaPublishService kafkaPublishService;
  @Mock
  private UpdateService updateService;
  private ProcessingService service;
  private JsonSchema schema;

  @BeforeEach
  void setup() throws IOException {
    this.service = new ProcessingService(cordraFeign, mapper, validationService, properties,
        kafkaPublishService, updateService);
    var factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
    this.schema = factory.getSchema(loadResourceFileToString("schema.json"));
  }

  @Test
  void testNewObject() throws Exception {
    // Given
    var message = loadResourceFile("test-object.json");
    given(cordraFeign.searchSingle(anyString())).willReturn(
        loadResourceFileToString("test-empty-search.json"));
    given(validationService.retrieveSchema(anyString())).willReturn(schema);
    given(properties.getType()).willReturn("ODStypeV0.2-Test");
    var json = mapper.readTree(loadResourceFile("test-object-full.json"));

    // When
    var result = service.processItem(createCloudEvent(message));

    // Then
    assertThat(result.get()).isEqualTo(json);
    then(kafkaPublishService).should().sendMessage(any(OpenDSWrapper.class), eq("anyQueue"));
  }

  @Test
  void testEqualObject() throws Exception {
    // Given
    var message = loadResourceFile("test-object.json");
    given(cordraFeign.searchSingle(anyString())).willReturn(
        loadResourceFileToString("test-search-equal.json"));
    given(properties.getType()).willReturn("ODStypeV0.2-Test");

    // When
    var result = service.processItem(createCloudEvent(message));

    // Then
    then(cordraFeign).shouldHaveNoMoreInteractions();
    assertThat(result.get()).isNull();
  }

  @Test
  void testUnequalObject() throws Exception {
    // Given
    var message = loadResourceFile("test-object.json");
    given(cordraFeign.searchSingle(anyString())).willReturn(
        loadResourceFileToString("test-search-unequal.json"));
    given(validationService.retrieveSchema(anyString())).willReturn(schema);
    given(properties.getType()).willReturn("ODStypeV0.2-Test");
    var json = (ObjectNode) mapper.readTree(loadResourceFile("test-object-full.json"));
    json.put("id", "test/eab36efab0bf0e60dfe0");
    given(updateService.updateObject(any(OpenDSWrapper.class), anyString(),
        any(OpenDSWrapper.class))).willReturn(
        mapper.readValue(TestUtils.loadResourceFile("updateService/test-object.json"),
            OpenDSWrapper.class));

    // When
    var result = service.processItem(createCloudEvent(message));

    // Then
    then(cordraFeign).shouldHaveNoMoreInteractions();
    assertThat(result.get()).isEqualTo(json);
  }

  @Test
  void testInvalidJson() throws Exception {
    // Given
    var message = loadResourceFile("test-object-invalid.json");

    // When
    var result = service.processItem(createCloudEvent(message));

    // Then
    then(cordraFeign).shouldHaveNoInteractions();
    assertThatThrownBy(result::get).isInstanceOf(ExecutionException.class).hasRootCauseInstanceOf(
        UnrecognizedPropertyException.class);
  }

  @Test
  void testValidationError() throws Exception {
    // Given
    var message = loadResourceFile("test-object-invalid-schema.json");
    given(cordraFeign.searchSingle(anyString())).willReturn(
        loadResourceFileToString("test-empty-search.json"));
    given(validationService.retrieveSchema(anyString()))
        .willReturn(schema);
    given(properties.getType()).willReturn("ODStypeV0.2-Test");

    // When
    var result = service.processItem(createCloudEvent(message));

    // Then
    then(cordraFeign).shouldHaveNoMoreInteractions();
    assertThatThrownBy(result::get).isInstanceOf(ExecutionException.class).hasRootCauseInstanceOf(
        JsonValidationException.class);
  }

}
