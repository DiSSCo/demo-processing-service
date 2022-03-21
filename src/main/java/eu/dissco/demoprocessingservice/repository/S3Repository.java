package eu.dissco.demoprocessingservice.repository;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.transfer.Transfer;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.demoprocessingservice.Profiles;
import eu.dissco.demoprocessingservice.domain.OpenDSWrapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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
public class S3Repository {

  private final ObjectMapper mapper;
  private final TransferManager manager;

  @Async
  public CompletableFuture<Void> saveItems(List<OpenDSWrapper> item) {
    var uploads = new ArrayList<Upload>();
    for (OpenDSWrapper authoritative : item) {
      try {
        var byar = mapper.writeValueAsString(authoritative).getBytes(
            StandardCharsets.UTF_8);
        var metadata = new ObjectMetadata();
        metadata.setContentLength(byar.length);
        var upload = manager.upload("cordra-kubernetes",
            "processing-test/test-performance-3-" + authoritative.getAuthoritative()
                .getPhysicalSpecimenId() + ".json",
            new ByteArrayInputStream(byar), metadata);
        uploads.add(upload);
      } catch (AmazonServiceException | IOException ex) {
        log.error("Failed");
      }
    }
    var done = false;
    while (!done) {
      if (uploads.stream().allMatch(Transfer::isDone)) {
        done = true;
      }
    }
    log.info("Upload to S3 completed, {}", item.size());
    return CompletableFuture.completedFuture(null);
  }

}
