package eu.dissco.demoprocessingservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.dissco.demoprocessingservice.client.CordraFeign;
import eu.dissco.demoprocessingservice.domain.OpenDSWrapper;
import eu.dissco.demoprocessingservice.exception.AuthenticationException;
import eu.dissco.demoprocessingservice.properties.CordraProperties;
import java.util.Collection;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class CordraSendService {

  private final ObjectMapper mapper;
  private final CordraProperties properties;
  private final CordraFeign cordraFeign;

  public String commitUpsertObject(Collection<JsonNode> upsertObjects)
      throws AuthenticationException {
    var bearerToken = authenticate();
    if (bearerToken != null) {
      var array = mapper.createArrayNode();
      array.addAll(upsertObjects);
      log.info("Sending message to Cordra instance, total messages: {}", upsertObjects.size());
      String response = cordraFeign.postCordraObjects(array, "Bearer " + bearerToken);
      processResponse(upsertObjects, response);
      return response;
    } else {
      log.warn("Bearer token is empty!!!");
      throw new AuthenticationException("Failed to authenticate with Cordra");
    }
  }

  private void processResponse(Collection<JsonNode> upsertObjects, String response) {
    try {
      var success = mapper.readTree(response).get("success").asBoolean();
      if (success) {
        log.info("Successfully committed: {} objects", upsertObjects.size());
      } else {
        log.warn(response);
      }
    } catch (JsonProcessingException e) {
      log.error("Failed to handle response on inserted objects, assuming call failed", e);
    }
  }

  private String authenticate() {
    var node = mapper.createObjectNode();
    node.put("grant_type", "password");
    node.put("username", properties.getUsername());
    node.put("password", properties.getPassword());
    try {
      var bearer = mapper.readTree(cordraFeign.authenticate(node));
      if (bearer.has("access_token")) {
        return bearer.get("access_token").asText();
      } else if (bearer.has("error")) {
        log.error("Failed to authenticate, unable to make call. Response was: {}",
            bearer.toPrettyString());
      }
    } catch (JsonProcessingException e) {
      log.error("Failed to retrieve Bearer token, unable to make call", e);
    }
    return null;
  }
}
