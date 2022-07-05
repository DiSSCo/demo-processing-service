package eu.dissco.demoprocessingservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.demoprocessingservice.service.ProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class OpenDSControllerTest {

  //TODO fix test when new data model is implemented

  private final ObjectMapper mapper = new ObjectMapper();
  @Mock
  private ProcessingService processingService;
  @Mock
  private KeycloakPrincipal<KeycloakSecurityContext> principal;

  @Mock
  private KeycloakSecurityContext securityContext;

  @Mock
  private AccessToken accessToken;

  private Authentication authentication;
  private OpenDSController controller;

  @BeforeEach
  void setup() {
    this.controller = new OpenDSController(processingService);
  }


//  @Test
//  void testCreate() throws
//  @Test
//  void testCreate() throws IOException, AuthenticationException {
//    // Given
//    var message = loadResourceFile("test-object.json");
//    var json = mapper.readValue(loadResourceFile("test-object-full.json"), OpenDSWrapper.class);
//    given(processingService.handleMessages(any())).willReturn(List.of(json));
//    givenAuthentication();
//
//    // When
//    var result = controller.createOpenDS(authentication, createCloudEvent(message));
//
//    // Then
//    assertThat(result.getBody()).isEqualTo(
//        List.of(mapper.readValue(loadResourceFile("test-object-full-with-id.json"))));
//    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
//  }

//  @Test
//  void testNoUpdateNeeded() throws IOException {
//    // Given
//    var message = loadResourceFile("test-object.json");
//    given(processingService.processItem(any())).willReturn(
//        CompletableFuture.completedFuture(null));
//    givenAuthentication();
//
//    // When
//    var result = controller.createOpenDS(authentication, createCloudEvent(message));
//
//    // Then
//    assertThat(result.getBody()).isNull();
//    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
//  }
//
//  @Test
//  void testCreateThrowsException() throws IOException {
//    // Given
//    var message = loadResourceFile("test-object.json");
//    given(processingService.processItem(any())).willReturn(CompletableFuture.failedFuture(
//        new JsonValidationException("Failed to validate Json")));
//    givenAuthentication();
//    var exception = mapper.createObjectNode();
//    exception.put("exception",
//        "eu.dissco.demoprocessingservice.exception.JsonValidationException: "
//            + "Failed to validate Json");
//
//    // When
//    var result = controller.createOpenDS(authentication, createCloudEvent(message));
//
//    // Then
//    assertThat(result.getBody()).isEqualTo(exception);
//    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
//  }

  private void givenAuthentication(String... auths) {
    authentication = new TestingAuthenticationToken(principal, null, auths);
    given(principal.getKeycloakSecurityContext()).willReturn(securityContext);
    given(securityContext.getToken()).willReturn(accessToken);
    given(accessToken.getSubject()).willReturn("e2befba6-9324-4bb4-9f41-d7dfae4a44b0");
  }

}
