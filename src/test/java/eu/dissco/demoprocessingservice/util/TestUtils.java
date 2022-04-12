package eu.dissco.demoprocessingservice.util;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.core.io.ClassPathResource;

public class TestUtils {

  public static final String EVENT_ID = "6474f841-b548-48d9-9a53-a9fd3df84084";
  public static final String SERVICE_NAME = "translator-test-service";
  public static final String ENDPOINT = "https://endpoint.com";
  public static final String EVENT_TYPE = "dissco/translator-event";

  public static String loadResourceFileToString(String filename) throws IOException {
    return new String(loadResourceFile(filename), StandardCharsets.UTF_8);
  }

  public static byte[] loadResourceFile(String fileName) throws IOException {
    return new ClassPathResource(fileName).getInputStream().readAllBytes();
  }

  public static CloudEvent createCloudEvent(byte[] openDs) {
    return CloudEventBuilder.v1()
        .withId(EVENT_ID)
        .withSource(URI.create(ENDPOINT))
        .withSubject(SERVICE_NAME)
        .withTime(OffsetDateTime.now(ZoneOffset.UTC))
        .withType(EVENT_TYPE)
        .withDataContentType("application/json")
        .withData(openDs)
        .build();
  }

}
