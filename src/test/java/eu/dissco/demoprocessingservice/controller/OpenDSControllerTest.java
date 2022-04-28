package eu.dissco.demoprocessingservice.controller;

import static eu.dissco.demoprocessingservice.util.TestUtils.loadResourceFileToString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.demoprocessingservice.client.CordraFeign;
import eu.dissco.demoprocessingservice.service.CordraSendService;
import eu.dissco.demoprocessingservice.service.ProcessingService;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class OpenDSControllerTest {

  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
  @Mock
  private ProcessingService processingService;
  @Mock
  private CordraSendService cordraSendService;
  @Mock
  private CordraFeign cordraFeign;
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
    this.controller = new OpenDSController(mapper, processingService, cordraSendService,
        cordraFeign);
  }

  @Test
  void testSearch() throws IOException {
    // Given
    var searchJson = loadResourceFileToString("test-search-equal.json");
    given(cordraFeign.search(anyString())).willReturn(searchJson);
    givenAuthentication();

    // When
    var result = controller.searchQuery("id:test/eab36efab0bf0e60dfe0", authentication);

    // Then
    assertThat(result.getBody()).isEqualTo(searchJson);
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void testFindId() throws IOException {
    // Given
    var searchJson = loadResourceFileToString("test-object-full-with-id.json");
    given(cordraFeign.searchById(anyString())).willReturn(searchJson);
    givenAuthentication();
    var http = mock(HttpServletRequest.class);
    given(http.getRequestURI()).willReturn("/opends/objects/test/eab36efab0bf0e60dfe0");
    given(http.getContextPath()).willReturn("");

    // When
    var result = controller.findOpenDSById(http, authentication);

    // Then
    assertThat(result.getBody()).isEqualTo(searchJson);
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  private void givenAuthentication(String... auths) {
    authentication = new TestingAuthenticationToken(principal, null, auths);
    given(principal.getKeycloakSecurityContext()).willReturn(securityContext);
    given(securityContext.getToken()).willReturn(accessToken);
    given(accessToken.getSubject()).willReturn("e2befba6-9324-4bb4-9f41-d7dfae4a44b0");
  }

}
