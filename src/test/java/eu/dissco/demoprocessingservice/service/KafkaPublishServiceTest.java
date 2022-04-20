package eu.dissco.demoprocessingservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doAnswer;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.demoprocessingservice.domain.OpenDSWrapper;
import eu.dissco.demoprocessingservice.properties.ApplicationProperties;
import eu.dissco.demoprocessingservice.util.TestUtils;
import io.cloudevents.CloudEvent;
import java.io.IOException;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

@ExtendWith(MockitoExtension.class)
class KafkaPublishServiceTest {

  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

  @Mock
  private KafkaTemplate<String, CloudEvent> kafkaTemplate;
  @Mock
  private ListenableFuture<SendResult<String, CloudEvent>> listenableFuture;
  @Mock
  private SendResult<String, CloudEvent> sendResult;
  private ApplicationProperties properties = new ApplicationProperties();
  private KafkaPublishService service;

  @BeforeEach
  void setup() {
    this.service = new KafkaPublishService(mapper, kafkaTemplate, properties);
  }

  @Test
  void testSendMessage() throws IOException {
    // Given
    var recordMetadata = new RecordMetadata(new TopicPartition("testTopic", 1), 100L, 0, 0L, 0, 0);
    given(kafkaTemplate.send(anyString(), any(CloudEvent.class))).willReturn(listenableFuture);
    given(sendResult.getRecordMetadata()).willReturn(recordMetadata);
    doAnswer(invocation -> {
      ListenableFutureCallback callBack = invocation.getArgument(0);
      callBack.onSuccess(sendResult);
      assertThat(sendResult.getRecordMetadata().offset()).isEqualTo(100);
      return null;
    }).when(listenableFuture).addCallback(any(ListenableFutureCallback.class));
    var opends = mapper.readValue(TestUtils.loadResourceFile("updateService/test-object.json"),
        OpenDSWrapper.class);

    // When
    service.sendMessage(opends, "testTopic");

    // Then
    then(kafkaTemplate).should().send(eq("testTopic"), any(CloudEvent.class));
  }

}
