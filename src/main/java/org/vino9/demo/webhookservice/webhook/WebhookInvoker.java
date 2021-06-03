package org.vino9.demo.webhookservice.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.vino9.demo.webhookservice.data.WebhookRequest;
import org.vino9.demo.webhookservice.data.WebhookRequestRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Component
@Slf4j
public class WebhookInvoker {
  private RestTemplate template;
  private Random random = new Random();
  final WebhookRequestRepository repository;
  final ObjectMapper mapper;

  @Autowired
  public WebhookInvoker(WebhookRequestRepository repository, ObjectMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  public Long invoke(WebhookRequest request) {

    var success = callWebHookAndUpdateStatus(request);
    repository.save(request);
    return request.getId();
  }

  private boolean callWebHookAndUpdateStatus(WebhookRequest request) {
    var payload = extractPayload(request);
    String url = payload.get("url");
    String message = payload.get("message");

    int duration = random.nextInt(5000);
    log.info("sleeping for {} ms for message {}", duration, request.getMessageId().substring(0, 8));
    try {
      Thread.sleep(duration);
      if (duration > 2500) {
        request.markDone();
        return true;
      }
    } catch (InterruptedException e) {
      request.markFailed();
    }
    return false;
  }

  private Map<String, String> extractPayload(WebhookRequest request) {
    try {
      TypeReference<HashMap<String, String>> typeRef =
          new TypeReference<HashMap<String, String>>() {};
      return mapper.readValue(request.getPayload(), typeRef);
    } catch (JsonProcessingException e) {
      return null;
    }
  }
}
