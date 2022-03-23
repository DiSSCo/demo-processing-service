package eu.dissco.demoprocessingservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.dissco.demoprocessingservice.client.CordraFeign;
import eu.dissco.demoprocessingservice.domain.OpenDSWrapper;
import eu.dissco.demoprocessingservice.exception.JsonValidationException;
import eu.dissco.demoprocessingservice.exception.SchemaValidationException;
import eu.dissco.demoprocessingservice.properties.CordraProperties;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class CordraService {

  private final CordraFeign cordraFeign;
  private final ObjectMapper mapper;
  private final ValidationService validationService;
  private final CordraProperties properties;
  private final KafkaPublishService kafkaPublishService;

  @Async("processingThreadPoolTaskExecutor")
  public CompletableFuture<JsonNode> processItem(String message) {
    try {
      var object = mapper.readValue(message, OpenDSWrapper.class);
      object.setType(properties.getType());
      var existingObjectOptional = findExisting(
          object.getAuthoritative().getPhysicalSpecimenId());
      if (existingObjectOptional.isEmpty()) {
        return processNewObject(message, object);
      } else {
        return processExistingObject(message, object, existingObjectOptional.get());
      }
    } catch (JsonProcessingException e) {
      log.error("Unable to parse object: {}", message, e);
    } catch (SchemaValidationException e) {
      log.error("Unable to validate message: {}", message, e);
    }
    return CompletableFuture.completedFuture(null);
  }

  private CompletableFuture<JsonNode> processNewObject(String message,
      OpenDSWrapper object) throws SchemaValidationException, JsonProcessingException {
    var json = validate(message);
    if (object.getImages() != null && !object.getImages().isEmpty()) {
      kafkaPublishService.sendMessage(object, "images");
    }
    return CompletableFuture.completedFuture(wrapJson(json, null));
  }

  private CompletableFuture<JsonNode> processExistingObject(String message,
      OpenDSWrapper object, JsonNode existingObjectOptional)
      throws JsonProcessingException, SchemaValidationException {
    var existingObject = mapper.treeToValue(existingObjectOptional.get("content"),
        OpenDSWrapper.class);
    if (existingObject.equals(object)) {
      log.debug("Objects are equal, no action needed");
      return CompletableFuture.completedFuture(null);
    } else {
      log.debug("Objects are not equal, update existing object");
      var json = validate(message);
      return CompletableFuture.completedFuture(
          wrapJson(json, existingObjectOptional.get("id")));
    }
  }

  private ObjectNode validate(String message)
      throws SchemaValidationException, JsonProcessingException {
    var contentNode = (ObjectNode) mapper.readTree(message);
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

  private Optional<JsonNode> findExisting(String physicalSpecimenId)
      throws JsonProcessingException {
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
