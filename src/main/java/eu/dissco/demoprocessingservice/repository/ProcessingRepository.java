package eu.dissco.demoprocessingservice.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import eu.dissco.demoprocessingservice.domain.OpenDSWrapper;
import java.util.List;
import java.util.Optional;

public interface ProcessingRepository {

  void saveItems(List<OpenDSWrapper> objects);

  Optional<JsonNode> findExisting(String physicalSpecimenId) throws JsonProcessingException;

}
