package eu.dissco.demoprocessingservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.demoprocessingservice.domain.OpenDSWrapper;
import eu.dissco.demoprocessingservice.util.TestUtils;
import io.cloudevents.CloudEvent;
import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateServiceTest {

  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

  @Mock
  private CloudEvent event;

  private UpdateService service;

  private static Stream<Arguments> testEnrichmentUpdates() {
    return Stream.of(
        Arguments.of("updateService/test-object.json", "updateService/test-object.json", true),
        Arguments.of("updateService/test-object-no-image.json", "updateService/test-object.json",
            true),
        Arguments.of("updateService/test-object.json", "updateService/test-object-no-image.json",
            false),
        Arguments.of("updateService/test-object.json", "updateService/test-object-two-images.json",
            true),
        Arguments.of("updateService/test-object-two-images.json", "updateService/test-object.json",
            false),
        Arguments.of("updateService/test-object.json",
            "updateService/test-object-image-additional.json", true),
        Arguments.of("updateService/test-object-image-additional.json",
            "updateService/test-object.json", false),
        Arguments.of("updateService/test-object-image-additional.json",
            "updateService/test-object-image-additional.json", true),
        Arguments.of("updateService/test-object-image-additional.json",
            "updateService/test-object-image-additional-updated.json", true),
        Arguments.of("updateService/test-object.json",
            "updateService/test-object-new-authoratative.json", false)
    );
  }

  private static Stream<Arguments> testAuthoratativeUpdates() {
    return Stream.of(
        Arguments.of("updateService/test-object.json",
            "updateService/test-object-new-authoratative.json", true),
        Arguments.of("updateService/test-object.json", "updateService/test-object-unmapped.json",
            true),
        Arguments.of("updateService/test-object-unmapped.json", "updateService/test-object.json",
            false),
        Arguments.of("updateService/test-object-unmapped.json",
            "updateService/test-object-unmapped.json", false),
        Arguments.of("updateService/test-object-unmapped.json",
            "updateService/test-object-updated-unmapped.json", true)
    );
  }

  @BeforeEach
  void setup() {
    this.service = new UpdateService();
  }

  @ParameterizedTest
  @MethodSource("testEnrichmentUpdates")
  void testEnrichmentUpdates(String existingJson, String newJson, boolean newIsResult)
      throws IOException {
    // Given
    var existingObject = mapper.readValue(
        TestUtils.loadResourceFile(existingJson),
        OpenDSWrapper.class);
    var newObject = mapper.readValue(TestUtils.loadResourceFile(newJson),
        OpenDSWrapper.class);
    var type = "eu.dissco.enrichment.response.event";

    // When
    var result = service.updateObject(newObject, type, existingObject);

    // Then
    if (newIsResult) {
      assertThat(result).isEqualTo(newObject);
    } else {
      assertThat(result).isEqualTo(existingObject);
    }
  }

  @Test
  void testMergeAdditionalInfo() throws IOException {
    // Given
    var existingObject = mapper.readValue(
        TestUtils.loadResourceFile("updateService/test-object-image-additional.json"),
        OpenDSWrapper.class);
    var newObject = mapper.readValue(
        TestUtils.loadResourceFile("updateService/test-object-image-additional-other.json"),
        OpenDSWrapper.class);
    var type = "eu.dissco.enrichment.response.event";
    var expected = mapper.readValue(
        TestUtils.loadResourceFile("updateService/test-object-image-additional-combined.json"),
        OpenDSWrapper.class);

    // When
    var result = service.updateObject(newObject, type, existingObject);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  @ParameterizedTest
  @MethodSource("testAuthoratativeUpdates")
  void testAuthoratativeUpdates(String existingJson, String newJson, boolean newIsResult)
      throws IOException {
    // Given
    var existingObject = mapper.readValue(
        TestUtils.loadResourceFile(existingJson),
        OpenDSWrapper.class);
    var newObject = mapper.readValue(TestUtils.loadResourceFile(newJson),
        OpenDSWrapper.class);
    var type = "eu.dissco.translator.event";

    // When
    var result = service.updateObject(newObject, type, existingObject);

    // Then
    if (newIsResult) {
      assertThat(result).isEqualTo(newObject);
    } else {
      assertThat(result).isEqualTo(existingObject);
    }
  }

}
