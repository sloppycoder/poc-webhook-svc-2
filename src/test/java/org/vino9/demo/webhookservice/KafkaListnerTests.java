package org.vino9.demo.webhookservice;

import lombok.extern.slf4j.Slf4j;
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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@DirtiesContext
@Slf4j
@EmbeddedKafka(
    partitions = 1,
    brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"})
class KafkaListnerTests {

  @Autowired private KafkaTemplate<String, WebhookRequest> template;

  @MockBean private WebhookInvoker invoker;

  @Value("${webhook.topic-pattern}")
  String testTopic;

  @Test
  void contextLoads() {}

  @Test
  void message_triggers_webhook_invoker() throws Exception {
    var request = RequestUtils.genDummyRequest(testTopic, "testing");
    template.send(testTopic, request);
    Thread.sleep(3000L);
    verify(invoker, times(1)).invoke(request);
  }
}
