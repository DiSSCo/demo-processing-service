package eu.dissco.demoprocessingservice.service;

import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import eu.dissco.demoprocessingservice.exception.MissingSchemaException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.cnri.cordra.api.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class ValidationService {

    private final CordraClient cordraClient;

    @Cacheable("validation")
    public JsonSchema retrieveSchema(String type) throws MissingSchemaException {
        var schema = searchSchema(type);
        var optionalObject = schema.stream().findFirst();
        if (optionalObject.isPresent()) {
            var factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
            return factory.getSchema(optionalObject.get().content.getAsJsonObject().get("schema").toString());
        } else {
            throw new MissingSchemaException("No schema found in Cordra, ensure that type: " + type + " is present");
        }
    }

    private SearchResults<CordraObject> searchSchema(String type) throws MissingSchemaException {
        SearchResults<CordraObject> schema;
        try {
            schema = cordraClient.search("/name:\"" + type + "\"", new QueryParams(0, 1));
        } catch (CordraException e) {
            throw new MissingSchemaException("Exception occurred during retrieval of schema", e);
        }
        return schema;
    }
}
