package eu.dissco.demoprocessingservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonParser;
import eu.dissco.demoprocessingservice.domain.OpenDSWrapper;
import eu.dissco.demoprocessingservice.exception.JsonValidationException;
import eu.dissco.demoprocessingservice.exception.SchemaValidationException;
import eu.dissco.demoprocessingservice.properties.CordraProperties;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.cnri.cordra.api.CordraClient;
import net.cnri.cordra.api.CordraException;
import net.cnri.cordra.api.CordraObject;
import net.cnri.cordra.api.QueryParams;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class CordraService {

  private final CordraClient cordraClient;
  private final ObjectMapper mapper;
  private final ValidationService validationService;
  private final CordraProperties properties;

  @Async
  public CompletableFuture<OpenDSWrapper> processItem(String message) {
    try {
      var object = mapper.readValue(message, OpenDSWrapper.class);
      var existingObjectOptional = findExisting(
          object.getAuthoritative().getPhysicalSpecimenId());
      if (existingObjectOptional.isEmpty()) {
        validate(message);
        return CompletableFuture.completedFuture(object);
      } else {
        var existingObject = mapper.readValue(existingObjectOptional.get().content.toString(),
            OpenDSWrapper.class);
        if (existingObject.equals(object)) {
          log.info("Objects are equal, no action needed");
        } else {
          log.info("Objects are not equal, update existing object");
          // TODO: Update logic
        }
      }
    } catch (JsonProcessingException e) {
      log.error("Unable to parse object: {}", message, e);
    } catch (CordraException e) {
      log.error("Unable to check Cordra for object: {}", message, e);
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

  private Optional<CordraObject> findExisting(String physicalSpecimenId) throws CordraException {
    var result = cordraClient.search(
        "\\@type:\"" + properties.getType()
            + "\" AND /ods\\:authoritative/ods\\:physicalSpecimenId:\""
            + physicalSpecimenId + "\"",
        new QueryParams(0, 1));
    return result.stream().findFirst();
  }

}
