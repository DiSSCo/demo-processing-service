package eu.dissco.demoprocessingservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonParser;
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

  @Async
  public CompletableFuture<OpenDSWrapper> processItem(String message) {
    try {
      var object = mapper.readValue(message, OpenDSWrapper.class);
      object.setType(properties.getType());
      var existingObjectOptional = findExisting(
          object.getAuthoritative().getPhysicalSpecimenId());
      if (existingObjectOptional.isEmpty()) {
        validate(message);
        if (object.getImages() != null && !object.getImages().isEmpty()) {
          kafkaPublishService.sendMessage(object, "images");
        }
        return CompletableFuture.completedFuture(object);
      } else {
        var existingObject = mapper.treeToValue(existingObjectOptional.get().get("content"),
            OpenDSWrapper.class);
        if (existingObject.equals(object)) {
          log.debug("Objects are equal, no action needed");
        } else {
          log.debug("Objects are not equal, update existing object");
          validate(message);
          object.setId(existingObjectOptional.get().get("id").asText());
          return CompletableFuture.completedFuture(object);
        }
      }
    } catch (JsonProcessingException e) {
      log.error("Unable to parse object: {}", message, e);
    } catch (SchemaValidationException e) {
      log.error("Unable to validate message: {}", message, e);
    }
    return CompletableFuture.completedFuture(null);
  }

  private void validate(String message)
      throws SchemaValidationException, JsonProcessingException {
    var jsonObject = JsonParser.parseString(message).getAsJsonObject();
    jsonObject.addProperty("@type", properties.getType());
    validateJson(jsonObject.toString());
  }

  private void validateJson(String json) throws SchemaValidationException, JsonProcessingException {
    var schema = validationService.retrieveSchema(properties.getType());
    var errors = schema.validate(mapper.readTree(json));
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

}
