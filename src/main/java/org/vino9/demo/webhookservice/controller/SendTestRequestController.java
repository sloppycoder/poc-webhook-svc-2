package org.vino9.demo.webhookservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.vino9.demo.webhookservice.data.WebhookRequest;
import org.vino9.demo.webhookservice.webhook.RequestUtils;

@RestController
@Slf4j
public class SendTestRequestController {
  private static final String TEST_TOPIC = "test-webhook-topic-1";

  private final KafkaTemplate<String, WebhookRequest> template;

  @Autowired
  public SendTestRequestController(KafkaTemplate<String, WebhookRequest> template) {
    this.template = template;
  }

  @GetMapping("/send")
  void sendTestMessage(@RequestParam("msg") String message, @RequestParam(defaultValue=TEST_TOPIC) String topic) {
    template.send(topic, RequestUtils.genDummyRequest(topic, message));
  }
}
