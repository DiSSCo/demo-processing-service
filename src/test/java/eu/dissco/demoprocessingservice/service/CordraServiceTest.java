package eu.dissco.demoprocessingservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import eu.dissco.demoprocessingservice.properties.CordraProperties;
import net.cnri.cordra.api.CordraClient;
import net.cnri.cordra.api.CordraException;
import net.cnri.cordra.api.CordraObject;
import net.cnri.cordra.api.QueryParams;
import net.cnri.cordra.collections.SearchResultsFromStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.stream.Stream;

import static eu.dissco.demoprocessingservice.util.TestUtils.loadResourceFile;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class CordraServiceTest {

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final int OFFSET = 100;

    @Mock
    private CordraClient cordraClient;
    @Mock
    private ValidationService validationService;
    @Mock
    private CordraProperties properties;
    private CordraService service;
    private JsonSchema schema;

    @BeforeEach
    void setup() throws IOException {
        this.service = new CordraService(cordraClient, mapper, validationService, properties);
        var factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        this.schema = factory.getSchema(loadResourceFile("schema.json"));
    }

    @Test
    void testNewObject() throws Exception {
        // Given
        var message = loadResourceFile("test-object.json");
        var searchResults = new SearchResultsFromStream(1, Stream.empty());
        var createdObject = new CordraObject();
        createdObject.id = "1";
        given(cordraClient.search(anyString(), any(QueryParams.class))).willReturn(searchResults);
        given(cordraClient.create(any())).willReturn(createdObject);
        given(validationService.retrieveSchema(anyString())).willReturn(schema);
        given(properties.getType()).willReturn("ODStypeV0.2-Test");

        // When
        service.processItem(message, OFFSET);

        // Then
        then(cordraClient).should().create(any());
    }

    @Test
    void testEqualObject() throws Exception {
        // Given
        var message = loadResourceFile("test-object.json");
        var givenObject = givenObject(message);
        var searchResults = new SearchResultsFromStream(1, Stream.of(givenObject));
        given(cordraClient.search(anyString(), any(QueryParams.class))).willReturn(searchResults);

        // When
        service.processItem(message, OFFSET);

        // Then
        then(cordraClient).shouldHaveNoMoreInteractions();
    }

    @Test
    void testUnequalObject() throws Exception {
        // Given
        var message = loadResourceFile("test-object.json");
        var givenObject = givenObject(loadResourceFile("test-object-different.json"));
        var searchResults = new SearchResultsFromStream(1, Stream.of(givenObject));
        given(cordraClient.search(anyString(), any(QueryParams.class))).willReturn(searchResults);

        // When
        service.processItem(message, OFFSET);

        // Then
        then(cordraClient).shouldHaveNoMoreInteractions();
    }

    @Test
    void testInvalidJson() throws Exception {
        // Given
        var message = loadResourceFile("test-object-invalid.json");

        // When
        service.processItem(message, OFFSET);

        // Then
        then(cordraClient).shouldHaveNoInteractions();
    }

    @Test
    void testFailedSearch() throws Exception {
        // Given
        var message = loadResourceFile("test-object.json");
        given(cordraClient.search(anyString(), any(QueryParams.class))).willThrow(
                CordraException.class);

        // When
        service.processItem(message, OFFSET);

        // Then
        then(cordraClient).shouldHaveNoMoreInteractions();
    }

    @Test
    void testValidationError() throws Exception {
        // Given
        var message = loadResourceFile("test-object-invalid-schema.json");
        var searchResults = new SearchResultsFromStream(1, Stream.empty());
        var createdObject = new CordraObject();
        createdObject.id = "1";
        given(cordraClient.search(anyString(), any(QueryParams.class))).willReturn(searchResults);
        var factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        var schema = factory.getSchema(loadResourceFile("schema.json"));
        given(validationService.retrieveSchema(anyString()))
                .willReturn(schema);
        given(properties.getType()).willReturn("ODStypeV0.2-Test");

        // When
        service.processItem(message, OFFSET);

        // Then
        then(cordraClient).shouldHaveNoMoreInteractions();
    }

    private CordraObject givenObject(String message) {
        var cordraObject = new CordraObject();
        cordraObject.id = "1";
        cordraObject.setContent(message);
        cordraObject.type = "ODStypeV0.2-Test";
        return cordraObject;
    }

}
