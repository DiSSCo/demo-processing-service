package eu.dissco.demoprocessingservice.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import net.cnri.cordra.api.CordraClient;
import net.cnri.cordra.api.CordraObject;
import net.cnri.cordra.api.QueryParams;
import net.cnri.cordra.collections.SearchResultsFromStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

import static eu.dissco.demoprocessingservice.util.TestUtils.loadResourceFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {

    @Mock
    private CordraClient cordraClient;

    private ValidationService validationService;

    @BeforeEach
    void setup() {
        this.validationService = new ValidationService(cordraClient);
    }

    @Test
    void testRetrieveSchema() throws Exception {
        // Given
        var searchResults = new SearchResultsFromStream(1, Stream.of(givenCordraObject()));
        given(cordraClient.search(anyString(), any(QueryParams.class))).willReturn(searchResults);
        var factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        var expected = factory.getSchema(loadResourceFile("schema.json"));

        // When
        var result = validationService.retrieveSchema("type");

        // Then
        assertThat(result.toString()).hasToString(expected.toString());
    }

    @Test
    void testNoSchemaAvailable() throws Exception {
        // Given
        var searchResults = new SearchResultsFromStream(1, Stream.empty());
        given(cordraClient.search(anyString(), any(QueryParams.class))).willReturn(searchResults);
        var factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        var expected = factory.getSchema(loadResourceFile("schema.json"));

        // When
        var result = validationService.retrieveSchema("type");

        // Then
        assertThat(result.toString()).hasToString(expected.toString());
    }

    private CordraObject givenCordraObject() throws IOException {
        var schemaObject = new JsonObject();
        schemaObject.add("schema", JsonParser.parseString(loadResourceFile("schema.json")));
        CordraObject object = new CordraObject();
        object.type = "object";
        object.setContent(schemaObject);
        return object;
    }
}
