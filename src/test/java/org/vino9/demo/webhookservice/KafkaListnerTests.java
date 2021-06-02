package org.vino9.demo.webhookservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.vino9.demo.webhookservice.data.WebhookRequest;
import org.vino9.demo.webhookservice.webhook.RequestUtils;
import org.vino9.demo.webhookservice.webhook.WebhookInvoker;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/*
Notes for embedded Kafka:
1. the topic must be created when the broker starts, auto create topic does not always work
2. By default the broker starts with a random port, and bootstrap server are bound to ${spring.embedded.kafka.brokers}
3. To pass extra configuration to broker, add set borkerProperties attribute in @EmbeddedKafka annotation
    brokerProperties = {"listeners=PLAINTEXT://127.0.0.1:9092", "port=9092"}
 */
@SpringBootTest
@DirtiesContext
@EmbeddedKafka(
    partitions = 1,
    topics = {"${webhook.topic-pattern}"})
class KafkaListnerTests {

  @Autowired private KafkaTemplate<String, WebhookRequest> template;
  @MockBean private WebhookInvoker invoker;

  @Value("${webhook.topic-pattern}")
  String testTopic;

  @Test
  void contextLoads() {}

  @Test
  void message_triggers_webhook_invoker() throws InterruptedException {
    Thread.sleep(1000L);
    var request = RequestUtils.genDummyRequest(testTopic, "testing");
    template.send(testTopic, request);
    verify(invoker, timeout(5000L).times(1)).invoke(request);
  }
}
