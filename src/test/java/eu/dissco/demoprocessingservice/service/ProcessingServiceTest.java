package eu.dissco.demoprocessingservice.service;

import static eu.dissco.demoprocessingservice.util.TestUtils.loadResourceFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.demoprocessingservice.properties.ApplicationProperties;
import eu.dissco.demoprocessingservice.repository.DigitalSpecimenRepository;
import eu.dissco.demoprocessingservice.repository.ElasticSearchRepository;
import eu.dissco.demoprocessingservice.util.TestUtils;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessingServiceTest {

  private final ObjectMapper mapper = new ObjectMapper();
  @Mock
  private UpdateService updateService;
  @Mock
  private ApplicationProperties properties;
  @Mock
  private ElasticSearchRepository elasticSearchRepository;
  @Mock
  private DigitalSpecimenRepository repository;

  private ProcessingService processingService;


  @BeforeEach
  void setup() {
    this.processingService = new ProcessingService(mapper, repository, updateService, properties,
        elasticSearchRepository);
  }

  @Test
  void testHandleMessages() throws IOException {
    // Given
    var givenEvent = TestUtils.createCloudEvent(loadResourceFile("events/event.json"));
    given(repository.digitalSpecimen(any())).willReturn(List.of());
    given(properties.getDsType()).willReturn("TestType");

    // When
    var result = processingService.handleMessages(List.of(givenEvent));

    // Then
    assertThat(result).isNotEmpty();
  }

}
