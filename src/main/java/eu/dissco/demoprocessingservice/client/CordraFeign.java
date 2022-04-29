package eu.dissco.demoprocessingservice.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "cordra", url = "${cordra.host}")
public interface CordraFeign {

  @PostMapping("batchUpload")
  String postCordraObjects(ArrayNode objects, @RequestHeader("Authorization") String bearerToken);

  @PostMapping("auth/token")
  String authenticate(JsonNode json);

  @GetMapping("/search?query={query}")
  String search(@PathVariable String query);

  @GetMapping("/search?query={query}&pageNum=0&pageSize=1")
  String searchSingle(@PathVariable String query);

  @GetMapping("/objects/{id}")
  String searchById(@PathVariable String id);
}
