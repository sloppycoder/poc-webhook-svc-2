package org.vino9.demo.webhookservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.vino9.demo.webhookservice.data.WebhookRequest;
import org.vino9.demo.webhookservice.webhook.RequestUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RestController
@Slf4j
public class SendTestRequestController {
  private final KafkaTemplate<String, WebhookRequest> template;
  @Value("#{${webhook.external-url-mapppings}}")
  Map<String, String> topicMappings;

  ArrayList<String> topics;
  int nTopics;
  private Random random = new Random();

  @Autowired
  public SendTestRequestController(KafkaTemplate<String, WebhookRequest> template) {
    this.template = template;
  }

  @GetMapping("/send")
  public void sendTestMessage(@RequestParam(defaultValue = "5") int count) {
    generateRandomRequests(count).forEach(request -> template.send(request.getClientId(), request));
  }

  private List<WebhookRequest> generateRandomRequests(int count) {
    return IntStream.range(0, count)
        .mapToObj(n -> pickRandomTopic())
        .filter(Objects::nonNull)
        .map(
            topic ->
                RequestUtils.genDummyRequest(
                    topic,
                    topicMappings.get(topic),
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private String pickRandomTopic() {
    if (topics == null) {
      topics = new ArrayList<>(topicMappings.keySet());
      nTopics = topics.size();
    }
    int index = random.nextInt(nTopics);
    return topics.get(index);
  }
}
