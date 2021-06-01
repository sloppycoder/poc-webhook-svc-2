package org.vino9.demo.webhookservice.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.*;
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
import java.util.concurrent.Executors;

@Component
@Slf4j
public class RequestListener {
  @Value("${webhook.executor-thread-pool-size:1}")
  int size = 5;

  private ListeningExecutorService executor =
      MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(size));
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
  public void process(String message,
                      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                      Acknowledgment ack) {
    WebhookRequest request = null;
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

    // TODO: explore using CompletableFuture to replace this.
    WebhookRequest finalRequest = request;
    ListenableFuture<Long> savedId = executor.submit(() -> invoker.invoke(finalRequest));
    Futures.addCallback(
        savedId,
        new FutureCallback<Long>() {
          public void onSuccess(Long requestId) {
            log.info("webhook success. saved to database with id {}", requestId);
            ack.acknowledge();
          }

          public void onFailure(Throwable thrown) {
            log.info("webhook failed, ignore for now.");
            ack.acknowledge();
          }
        },
        executor);
  }
}
