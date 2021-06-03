package org.vino9.demo.webhookservice.webhook;

import org.vino9.demo.webhookservice.data.WebhookRequest;

import java.time.LocalDateTime;
import java.util.UUID;

public class RequestUtils {
  public static WebhookRequest genDummyRequest(String clientId, String url, String message) {
    var payload = String.format("{\"url\":\"%s\", \"message\": \"%s\"}", url, message);
    return WebhookRequest.builder()
        .clientId(clientId)
        .messageId(UUID.randomUUID().toString())
        .messageType("WEBHOOK")
        .payload(payload)
        .createdAt(LocalDateTime.now())
        .build();
  }
}
