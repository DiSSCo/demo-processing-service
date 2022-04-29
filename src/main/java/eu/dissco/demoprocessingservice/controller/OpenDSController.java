package eu.dissco.demoprocessingservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.demoprocessingservice.client.CordraFeign;
import eu.dissco.demoprocessingservice.exception.AuthenticationException;
import eu.dissco.demoprocessingservice.service.CordraSendService;
import eu.dissco.demoprocessingservice.service.ProcessingService;
import io.cloudevents.CloudEvent;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/opends")
@RequiredArgsConstructor
public class OpenDSController {

  private final ObjectMapper mapper;
  private final ProcessingService processingService;
  private final CordraSendService cordraSendService;
  private final CordraFeign cordraFeign;


  @PreAuthorize("isAuthenticated()")
  @GetMapping(value = "/objects/**", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> findOpenDSById(HttpServletRequest request,
      Authentication authentication) {
    var id = request.getRequestURI().split(request.getContextPath() + "/objects/")[1];
    log.info("Received get request for id: {}, from user: {}", id,
        getNameFromToken(authentication));
    return ResponseEntity.ok(cordraFeign.searchById(id));
  }

  @PreAuthorize("isAuthenticated()")
  @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> searchQuery(@RequestParam String query,
      Authentication authentication) {
    log.info("Received get request with query: {}, from user: {}", query,
        getNameFromToken(authentication));
    return ResponseEntity.ok(cordraFeign.search(query));
  }

  @PreAuthorize("isAuthenticated()")
  @PatchMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonNode> updateOpenDS(Authentication authentication,
      @RequestBody CloudEvent cloudEvent) {
    return createOpenDS(authentication, cloudEvent);
  }

  @PreAuthorize("isAuthenticated()")
  @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonNode> createOpenDS(Authentication authentication,
      @RequestBody CloudEvent cloudEvent) {
    log.info("Received cloudEvent with id: {} from user: {}", cloudEvent.getId(),
        getNameFromToken(authentication));
    var future = processingService.processItem(cloudEvent);
    try {
      var openDs = future.get();
      if (openDs != null) {
        var result = cordraSendService.commitUpsertObject(List.of(openDs));
        var resultJson = mapper.readTree(result);
        return ResponseEntity.ok().body(resultJson.get("results").get(0).get("response"));
      } else {
        log.error("No need to update the object, it is already in object storage");
        return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
      }
    } catch (ExecutionException | AuthenticationException | JsonProcessingException e) {
      log.error("Failed to insert event: {} with exception", cloudEvent.getId(), e);
      var objectNode = mapper.createObjectNode();
      objectNode.put("exception", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(objectNode);
    } catch (InterruptedException e) {
      log.error("Failed to insert event: {} with exception", cloudEvent.getId(), e);
      Thread.currentThread().interrupt();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  private String getNameFromToken(Authentication authentication) {
    KeycloakPrincipal<? extends KeycloakSecurityContext> principal =
        (KeycloakPrincipal<?>) authentication.getPrincipal();
    AccessToken token = principal.getKeycloakSecurityContext().getToken();
    return token.getSubject();
  }

}
