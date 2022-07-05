package eu.dissco.demoprocessingservice.service;

import static java.util.stream.Collectors.groupingBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.demoprocessingservice.domain.EventData;
import eu.dissco.demoprocessingservice.domain.OpenDSWrapper;
import eu.dissco.demoprocessingservice.properties.ApplicationProperties;
import eu.dissco.demoprocessingservice.repository.DigitalSpecimenRepository;
import eu.dissco.demoprocessingservice.repository.ElasticSearchRepository;
import io.cloudevents.CloudEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessingService {

  private final ObjectMapper mapper;
  private final DigitalSpecimenRepository repository;
  private final UpdateService updateService;
  private final ApplicationProperties properties;
  private final ElasticSearchRepository elasticSearchRepository;

  //TODO same object in batch twice
  @Transactional
  public List<OpenDSWrapper> handleMessages(List<CloudEvent> messages) throws IOException {
    List<OpenDSWrapper> updatedItems = checkUpdatedItems(messages);
    if (!updatedItems.isEmpty()) {
      commitToDataBase(updatedItems);
      elasticSearchRepository.commitToIndex(updatedItems);
      return updatedItems;
    } else {
      log.info("No updates to create or update");
      return Collections.emptyList();
    }
  }

  private void commitToDataBase(List<OpenDSWrapper> updatedItems) throws JsonProcessingException {
    log.info("Upserting all specimen: {}", updatedItems.size());
    repository.commitUpsertObject(updatedItems);
    log.info("Successfully committed specimen, now upserting images");
    var specimenWithImage = updatedItems.stream().filter(this::filterSpecimenWithoutImages)
        .toList();
    log.info("Committing {} specimen with images", specimenWithImage.size());
    repository.commitImages(specimenWithImage);
  }

  private List<OpenDSWrapper> checkUpdatedItems(List<CloudEvent> messages) {
    var dsList = messages.stream().map(this::mapMessages).filter(Objects::nonNull).toList();
    var curatedIds = dsList.stream().map(this::curatedId).toList();
    log.info("Requesting existingSpecimens");
    var existingSpecimen = repository.digitalSpecimen(curatedIds);
    log.info("Found: {} existing specimens", existingSpecimen.size());
    var mergedSpecimenList = Stream.of(dsList.stream().map(EventData::getOpenDS),
            existingSpecimen.stream()).flatMap(Function.identity())
        .collect(groupingBy(ds -> ds.getAuthoritative().getPhysicalSpecimenId()));
    return mergedSpecimenList.values().stream().map(this::update)
        .filter(Objects::nonNull)
        .toList();
  }

  private boolean filterSpecimenWithoutImages(OpenDSWrapper openDSWrapper) {
    return openDSWrapper.getImages() != null && !openDSWrapper.getImages().isEmpty();
  }

  private OpenDSWrapper update(List<OpenDSWrapper> openDSWrappers) {
    if (openDSWrappers.size() == 2) {
      return updateService.updateObject(openDSWrappers.get(0), "eu.dissco.translator.event",
          openDSWrappers.get(1));
    } else {
      var opends = openDSWrappers.get(0);
      opends.setId(retrieveHandle());
      opends.setType(properties.getDsType());
      return opends;
    }
  }

  private String retrieveHandle() {
    return "test/" + UUID.randomUUID();
  }

  private String curatedId(EventData eventData) {
    return eventData.getOpenDS().getAuthoritative().getPhysicalSpecimenId();
  }

  private EventData mapMessages(CloudEvent cloudEvent) {
    var data = cloudEvent.getData().toBytes();
    try {
      return mapper.readValue(data, EventData.class);
    } catch (IOException e) {
      log.error("Failed to parse event: {}", new String(data, StandardCharsets.UTF_8), e);
      return null;
    }
  }

}
