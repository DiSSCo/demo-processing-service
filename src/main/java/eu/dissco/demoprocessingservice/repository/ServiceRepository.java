package eu.dissco.demoprocessingservice.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import eu.dissco.demoprocessingservice.Profiles;
import eu.dissco.demoprocessingservice.domain.OpenDSWrapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@AllArgsConstructor
@Profile(Profiles.SERVICE)
public class ServiceRepository implements ProcessingRepository {

  private final ElasticRepository elasticRepository;
  private final S3Repository s3Repository;

  @Override
  public void saveItems(List<OpenDSWrapper> objects) {
    log.info("Saving a item set of: {}", objects.size());
    objects.forEach(openDSWrapper -> openDSWrapper.setId(UUID.randomUUID().toString()));
    var futures = new ArrayList<CompletableFuture<Void>>();
    futures.add(elasticRepository.saveItems(objects));
    futures.add(s3Repository.saveItems(objects));
    futures.stream().map(CompletableFuture::join).toList();
  }

  @Override
  public Optional<JsonNode> findExisting(String physicalSpecimenId) throws JsonProcessingException {
    return elasticRepository.findExisting(physicalSpecimenId);
  }
}
