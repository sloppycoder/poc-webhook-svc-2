package org.vino9.demo.webhookservice.webhook;

import org.vino9.demo.webhookservice.data.WebhookRequest;

import java.time.LocalDateTime;
import java.util.UUID;

public class RequestUtils {
  public static WebhookRequest genDummyRequest(String clientId, String message) {
    return WebhookRequest.builder()
        .clientId(clientId)
        .messageId(UUID.randomUUID().toString())
        .messageType("WEBHOOK")
        .payload(message)
        .createdAt(LocalDateTime.now())
        .build();
  }
}
