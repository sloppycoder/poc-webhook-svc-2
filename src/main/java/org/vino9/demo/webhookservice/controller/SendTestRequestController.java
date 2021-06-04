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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RestController
@Slf4j
public class SendTestRequestController {
  private final KafkaTemplate<String, WebhookRequest> template;
  @Value("#{${webhook.external-url-mapppings}}")
  Map<String, String> urlMappings;
  @Value("${webhook.topic}")
  String topic;
  private Random random = new Random();

  @Autowired
  public SendTestRequestController(KafkaTemplate<String, WebhookRequest> template) {
    this.template = template;
  }

  @GetMapping("/send")
  public void sendTestMessage(@RequestParam(defaultValue = "5") int count) {
    generateRandomRequests(count).forEach(request -> template.send(topic, request));
  }

  private List<WebhookRequest> generateRandomRequests(int count) {
    return IntStream.range(0, count)
        .mapToObj(n -> pickRandomClientId())
        .map(
            clientId ->
                RequestUtils.genDummyRequest(
                    clientId,
                    urlMappings.get(clientId),
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private String pickRandomClientId() {
    return String.format("client-%d", random.nextInt(2) + 1);
  }
}
