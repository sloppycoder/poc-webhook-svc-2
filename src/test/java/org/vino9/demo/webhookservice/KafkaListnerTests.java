package org.vino9.demo.webhookservice;

import org.junit.ClassRule;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import org.vino9.demo.webhookservice.data.WebhookRequest;
import org.vino9.demo.webhookservice.webhook.RequestListener;
import org.vino9.demo.webhookservice.webhook.RequestUtils;
import org.vino9.demo.webhookservice.webhook.WebhookInvoker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@DirtiesContext
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
class KafkaListnerTests {

    @ClassRule
    public static KafkaContainer kafka =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:5.4.3"));

    @Autowired
    private KafkaTemplate<String, WebhookRequest> template;

    @Mock
    private WebhookInvoker invoker;

    @Value("${webhook.topic-pattern}")
    String testTopic;

    @Test
    void message_triggers_webhook_invoker() throws Exception {
        var request = RequestUtils.genDummyRequest(testTopic, "testing");
        template.send(testTopic, request);
        Thread.sleep(9000L);
        verify(invoker, times(1)).invoke(request);
    }
}
