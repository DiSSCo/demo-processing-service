package eu.dissco.demoprocessingservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.dissco.demoprocessingservice.domain.Authoritative;
import java.util.Optional;
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

  @Async
  public void processItem(String json, int offset) {
    try {
      var object = mapper.readValue(json, Authoritative.class);
      var existingObjectOptional = findExisting(object.getPhysicalSpecimenId());
      if (existingObjectOptional.isEmpty()) {
        saveItem(json);
      } else {
        var existingObject = mapper.readValue(existingObjectOptional.get().content.toString(),
            Authoritative.class);
        if (existingObject.equals(object)) {
          log.debug("Objects are equal, no action needed");
        } else {
          log.debug("Objects are not equal, update existing object");
          // TODO: Update logic
        }
      }
    } catch (JsonProcessingException e) {
      log.error("Unable to parse object: {}", json, e);
    } catch (CordraException e) {
      log.error("Unable to check Cordra for object: {}", json, e);
    }
    if (offset % 100 == 0) {
      log.info("Currently at offset: {}", offset);
    }
  }

  private Optional<CordraObject> findExisting(String physicalSpecimenId) throws CordraException {
    var result = cordraClient.search(
        "\\@type:\"ODStypeV0.2-Test\" AND /ods\\:authoritative/ods\\:physicalSpecimenId:\""
            + physicalSpecimenId + "\"",
        new QueryParams(0, 1));
    return result.stream().findFirst();
  }

  private void saveItem(String json) {
    var jsonObject = new JsonObject();
    jsonObject.add("ods:authoritative", JsonParser.parseString(json));
    jsonObject.addProperty("@type", "ODStypeV0.2-Test");
    var cordraObject = new CordraObject("ODStypeV0.2-Test", jsonObject);
    try {
      var cordra = cordraClient.create(cordraObject);
      log.debug("Successfully insert object: {}", cordra.id);
    } catch (CordraException e) {
      log.error("Failed to insert object", e);
    }
  }

}
