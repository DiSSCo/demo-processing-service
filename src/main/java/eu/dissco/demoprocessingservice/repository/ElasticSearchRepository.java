package eu.dissco.demoprocessingservice.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import eu.dissco.demoprocessingservice.domain.OpenDSWrapper;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ElasticSearchRepository {

  private final ElasticsearchClient elasticsearchClient;

  public void commitToIndex(List<OpenDSWrapper> updatedItems) throws IOException {
    log.info("Indexing all specimen: {}", updatedItems.size());
    var bulkRequest = new BulkRequest.Builder();
    for (OpenDSWrapper updatedItem : updatedItems) {
      bulkRequest.operations(op ->
          op.index(idx -> idx
              .index("dissco")
              .id(updatedItem.getId())
              .document(updatedItem)
          )
      );
    }
    log.info("Sending data to Elastic");
    elasticsearchClient.bulk(bulkRequest.build());
  }
}
