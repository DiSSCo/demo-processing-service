package eu.dissco.demoprocessingservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.dissco.demoprocessingservice.client.CordraFeign;
import eu.dissco.demoprocessingservice.domain.Enrichment;
import eu.dissco.demoprocessingservice.domain.EventData;
import eu.dissco.demoprocessingservice.domain.Image;
import eu.dissco.demoprocessingservice.domain.OpenDSWrapper;
import eu.dissco.demoprocessingservice.exception.JsonValidationException;
import eu.dissco.demoprocessingservice.exception.SchemaValidationException;
import eu.dissco.demoprocessingservice.properties.CordraProperties;
import io.cloudevents.CloudEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class ProcessingService {

  private final CordraFeign cordraFeign;
  private final ObjectMapper mapper;
  private final ValidationService validationService;
  private final CordraProperties properties;
  private final KafkaPublishService kafkaPublishService;
  private final UpdateService updateService;

  @Async("processingThreadPoolTaskExecutor")
  public CompletableFuture<JsonNode> processItem(CloudEvent message) {
    try {
      var data = message.getData();
      if (data == null) {
        log.error("Event does not contain data: {}", message);
        return CompletableFuture.completedFuture(null);
      }
      var event = mapper.readValue(data.toBytes(), EventData.class);
      event.getOpenDS().setType(properties.getType());
      var existingObjectOptional = findExisting(event.getOpenDS());
      if (existingObjectOptional.isEmpty()) {
        return processNewObject(event);
      } else {
        return processExistingObject(event.getOpenDS(), existingObjectOptional.get(), message);
      }
    } catch (SchemaValidationException e) {
      log.error("Unable to validate message: {}", message, e);
    } catch (IOException e) {
      log.error("Unable to parse object: {}", message, e);
    }
    return CompletableFuture.completedFuture(null);
  }

  private CompletableFuture<JsonNode> processNewObject(EventData event)
      throws SchemaValidationException, IOException {
    var json = validate(event.getOpenDS());
    if (event.getEnrichment() != null) {
      for (var enrichment : event.getEnrichment()) {
        if (onlyImages(event, enrichment)) {
          kafkaPublishService.sendMessage(event.getOpenDS(), enrichment.getName());
        } else if (!enrichment.isImageOnly()) {
          kafkaPublishService.sendMessage(event.getOpenDS(), enrichment.getName());
        }
      }
    }
    return CompletableFuture.completedFuture(wrapJson(json, null));
  }

  private boolean onlyImages(EventData event, Enrichment enrichment) {
    return enrichment.isImageOnly() &&
        event.getOpenDS().getImages() != null &&
        !event.getOpenDS().getImages().isEmpty();
  }

  private CompletableFuture<JsonNode> processExistingObject(OpenDSWrapper newObject,
      JsonNode existingObjectOptional, CloudEvent message)
      throws IOException, SchemaValidationException {
    var existingObject = mapper.treeToValue(existingObjectOptional.get("content"),
        OpenDSWrapper.class);
    if (existingObject.equals(newObject)) {
      log.debug("Objects are equal, no action needed");
      return CompletableFuture.completedFuture(null);
    } else {
      var object = updateService.updateObject(newObject, message, existingObject);
      var json = validate(object);
      return CompletableFuture.completedFuture(
          wrapJson(json, existingObjectOptional.get("id")));
    }
  }

  private ObjectNode validate(OpenDSWrapper message)
      throws SchemaValidationException {
    var contentNode = mapper.convertValue(message, ObjectNode.class);
    contentNode.put("@type", properties.getType());
    validateJson(contentNode);
    return contentNode;
  }

  private void validateJson(ObjectNode json) throws SchemaValidationException {
    var schema = validationService.retrieveSchema(properties.getType());
    var errors = schema.validate(json);
    if (!errors.isEmpty()) {
      errors.forEach(error -> log.error("Schema validation failed with error: {}", error));
      throw new JsonValidationException("Failed to validate Json");
    }
  }

  private Optional<JsonNode> findExisting(OpenDSWrapper openDSWrapper)
      throws JsonProcessingException {
    var physicalSpecimenId = openDSWrapper.getAuthoritative().getPhysicalSpecimenId();
    if (physicalSpecimenId == null) {
      return Optional.empty();
    }
    // TODO escape all necessary parameters
    var escapedPhysicalSpecimenId = physicalSpecimenId.replace("\\", "\\\\");
    var query = "type:\"" + properties.getType() + "\" AND " +
        "/ods\\:authoritative/ods\\:physicalSpecimenId:\"" + escapedPhysicalSpecimenId + "\"";
    var result = cordraFeign.search(query);
    var firstResult = mapper.readTree(result).get("results").get(0);
    if (firstResult == null || firstResult.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(firstResult);
  }

  private ObjectNode wrapJson(ObjectNode content, JsonNode id) {
    var object = mapper.createObjectNode();
    var contentNode = (ObjectNode) mapper.valueToTree(content);
    contentNode.put("@type", properties.getType());
    object.put("type", properties.getType());
    object.set("content", contentNode);
    if (id != null) {
      object.set("id", id);
    }
    return object;
  }

}
