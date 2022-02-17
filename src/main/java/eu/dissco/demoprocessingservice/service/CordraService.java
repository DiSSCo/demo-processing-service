package eu.dissco.demoprocessingservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.dissco.demoprocessingservice.domain.Authoritative;
import eu.dissco.demoprocessingservice.domain.OpenDSWrapper;
import eu.dissco.demoprocessingservice.exception.JsonValidationException;
import eu.dissco.demoprocessingservice.exception.SchemaValidationException;
import eu.dissco.demoprocessingservice.properties.CordraProperties;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.cnri.cordra.api.CordraClient;
import net.cnri.cordra.api.CordraException;
import net.cnri.cordra.api.CordraObject;
import net.cnri.cordra.api.QueryParams;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class CordraService {

    private final CordraClient cordraClient;
    private final ObjectMapper mapper;
    private final ValidationService validationService;
    private final CordraProperties properties;

    @Async
    public void processItem(String json, int offset) {
        try {
            var object = mapper.readValue(json, OpenDSWrapper.class);
            var existingObjectOptional = findExisting(object.getAuthoritative().getPhysicalSpecimenId());

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
        } catch (SchemaValidationException e) {
            log.error("Unable to validate message: {}", json, e);
        }
        if (offset % 100 == 0) {
            log.info("Currently at offset: {}", offset);
        }
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
                "\\@type:\"" + properties.getType() + "\" AND /ods\\:authoritative/ods\\:physicalSpecimenId:\""
                        + physicalSpecimenId + "\"",
                new QueryParams(0, 1));
        return result.stream().findFirst();
    }

    private void saveItem(String json) throws SchemaValidationException, JsonProcessingException {
        var jsonObject = JsonParser.parseString(json).getAsJsonObject();
        jsonObject.addProperty("@type", properties.getType());
        validateJson(jsonObject.toString());
        var cordraObject = new CordraObject(properties.getType(), jsonObject);
        try {
            var cordra = cordraClient.create(cordraObject);
            log.debug("Successfully insert object: {}", cordra.id);
        } catch (CordraException e) {
            log.error("Failed to insert object", e);
        }
    }

}
