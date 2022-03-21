package eu.dissco.demoprocessingservice.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.dissco.demoprocessingservice.Profiles;
import eu.dissco.demoprocessingservice.domain.OpenDSWrapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@AllArgsConstructor
@Profile(Profiles.SERVICE)
public class ElasticRepository {

  private final ElasticsearchClient elasticClient;

  @Async
  public CompletableFuture<Void> saveItems(List<OpenDSWrapper> items) {
    var operations = new ArrayList<BulkOperation>();
    for (OpenDSWrapper item : items) {
      var bulkRequest = new BulkOperation.Builder()
          .index(i -> i.document(item).id(item.getId()))
          .build();
      operations.add(bulkRequest);
    }
    var bulk = new BulkRequest.Builder()
        .index("processing-test-3")
        .operations(operations).build();
    try {
      var result = elasticClient.bulk(bulk);
      log.info("Adding result took: {}", result.took());
      if(result.errors()){
        log.warn("Bulk request had errors");
        for (BulkResponseItem item : result.items()) {
          if (item.error() != null){
            log.error("Item: {} has error: {}", item, item.error().stackTrace());
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    log.info("Upload to Elastic completed, {}", operations.size());
    return CompletableFuture.completedFuture(null);
  }

  public Optional<JsonNode> findExisting(String physicalSpecimenId) {
    var query = new Query.Builder()
        .match(
            m -> m.query(physicalSpecimenId)
                .field("ods:authoritative.ods:physicalSpecimenId")
                .minimumShouldMatch("100%"))
        .build();
    var searchRequest = new SearchRequest.Builder()
        .query(query)
        .index("processing-test-3")
        .size(1)
        .build();
    try {
      var result = elasticClient.search(searchRequest, JsonNode.class);
      if (result.hits().total().value() != 0) {
        var hit = result.hits().hits().get(0);
        if (!hit.source()
            .get("ods:authoritative")
            .get("ods:physicalSpecimenId").asText()
            .equals(physicalSpecimenId)) {
          log.warn("Unexpected hit for: {}", physicalSpecimenId);
        } else {
          var node = (ObjectNode) result.hits().hits().get(0).source();
          node.put("id", result.hits().hits().get(0).id());
          return Optional.of(node);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return Optional.empty();
  }

}
