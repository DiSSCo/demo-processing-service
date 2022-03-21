package eu.dissco.demoprocessingservice.configuration;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.demoprocessingservice.Profiles;
import lombok.AllArgsConstructor;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@AllArgsConstructor
@Profile(Profiles.SERVICE)
public class ElasticSearchConfiguration {

  private final ObjectMapper mapper;

  @Bean
  public ElasticsearchClient elasticsearchClient() {
    // Create the low-level client
    RestClient restClient = RestClient.builder(
        new HttpHost("localhost", 9200)).build();

    // Create the transport with a Jackson mapper
    ElasticsearchTransport transport = new RestClientTransport(
        restClient, new JacksonJsonpMapper(mapper));

    // And create the API client
    return new ElasticsearchClient(transport);
  }

}
