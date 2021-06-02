package org.vino9.demo.webhookservice.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.vino9.demo.webhookservice.data.WebhookRequest;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Slf4j
public class RequestListener {
  @Value("${webhook.executor-thread-pool-size:1}")
  int size = 5;

  private ExecutorService executor = Executors.newFixedThreadPool(size);
  private WebhookInvoker invoker;
  private final ObjectMapper mapper;

  @Autowired
  public RequestListener(WebhookInvoker invoker, ObjectMapper mapper) {
    this.invoker = invoker;
    this.mapper = mapper;
  }

  // topicPattern is a java.util.regex.Pattern
  @KafkaListener(
      concurrency = "1",
      groupId = "webhook-listner",
      topicPattern = "${webhook.topic-pattern}")
  public void process(
      String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic, Acknowledgment ack) {
    WebhookRequest request;
    try {
      request = mapper.readValue(message, WebhookRequest.class);
    } catch (JsonProcessingException e) {
      // invalid payload, for experiment sake, we just create a new request anyways.
      request =
          WebhookRequest.builder()
              .clientId(topic)
              .messageId(UUID.randomUUID().toString())
              .messageType("WEBHOOK")
              .createdAt(LocalDateTime.now())
              .build();
    }

    log.info("before invokerWebhookAsync");
    invokerWebhookAsync(request, ack);
    log.info("after invokerWebhookAsync");
  }

  private void invokerWebhookAsync(WebhookRequest request, Acknowledgment ack) {
    var messageId = request.getMessageId().substring(0, 8);
    var futureId = new CompletableFuture<Long>();
    executor.submit(
        () -> {
          try {
            var id = invoker.invoke(request);
            futureId.complete(id);
          } catch (RuntimeException ex) {
            futureId.completeExceptionally(ex);
          }
        });

    futureId
        .thenApply(
                id -> {
              log.info("webhook success for message {}, saved to database with id {}", messageId, id);
              ack.acknowledge();
              return id;
            })
        .handle(
            (s, ex) -> {
              log.info(
                  "webhook failed for message {} due to {}, ignore for now",
                  messageId,
                  ex.getCause().getMessage());
              ack.acknowledge();
              return s;
            });
  }
}
