package eu.dissco.demoprocessingservice.controller;

import eu.dissco.demoprocessingservice.domain.OpenDSWrapper;
import eu.dissco.demoprocessingservice.service.ProcessingService;
import io.cloudevents.CloudEvent;
import java.io.IOException;
import java.util.List;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/opends")
@RequiredArgsConstructor
public class OpenDSController {

  private final ProcessingService processingService;

  @PreAuthorize("isAuthenticated()")
  @PatchMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<OpenDSWrapper> updateOpenDS(Authentication authentication,
      @RequestBody CloudEvent cloudEvent) throws IOException {
    return createOpenDS(authentication, cloudEvent);
  }

  @PreAuthorize("isAuthenticated()")
  @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<OpenDSWrapper> createOpenDS(Authentication authentication,
      @RequestBody CloudEvent cloudEvent) throws IOException {
    log.info("Received cloudEvent with id: {} from user: {}", cloudEvent.getId(),
        getNameFromToken(authentication));
    var result = processingService.handleMessages(List.of(cloudEvent));
    if (result.isEmpty()) {
      log.error("No need to update the object, it is already in object storage");
      return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
    } else {
      return ResponseEntity.ok(result.get(0));
    }
  }

  private String getNameFromToken(Authentication authentication) {
    KeycloakPrincipal<? extends KeycloakSecurityContext> principal =
        (KeycloakPrincipal<?>) authentication.getPrincipal();
    AccessToken token = principal.getKeycloakSecurityContext().getToken();
    return token.getSubject();
  }

}
