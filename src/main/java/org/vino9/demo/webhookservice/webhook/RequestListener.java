package org.vino9.demo.webhookservice.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.influx.InfluxMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.vino9.demo.webhookservice.data.WebhookRequest;

import javax.annotation.PostConstruct;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class RequestListener {
  private final InfluxMeterRegistry registry;

  @Value("${webhook.executor-thread-pool-size:1}")
  int size;

  private ConcurrentHashMap<String, Long> stats = new ConcurrentHashMap<>();
  private ExecutorService executor;
  private WebhookInvoker invoker;
  private ObjectMapper mapper;

  @Autowired
  public RequestListener(WebhookInvoker invoker, ObjectMapper mapper, InfluxMeterRegistry registry) {
    this.invoker = invoker;
    this.mapper = mapper;
    this.registry = registry;
  }

  @KafkaListener(
      concurrency = "2",
      groupId = "webhook-listener",
      topics = {"${webhook.topic}"})
  public void process(
      String message, Acknowledgment ack, ConsumerRecord<String, Object> consumerRecord) {
    log.info("received message."); // check thread id for log output
    updateTopicStats(consumerRecord);

    WebhookRequest request;
    try {
      request = mapper.readValue(message, WebhookRequest.class);
      invokerWebhookAsync(request, ack);
    } catch (JsonProcessingException e) {
      // TODO: put into DLQ topic?
      log.info("invalid message received");
      ack.acknowledge();
    }
  }

  private void invokerWebhookAsync(WebhookRequest request, Acknowledgment ack) {
    var messageId = request.getMessageId().substring(0, 8);
    var futureRequest = new CompletableFuture<WebhookRequest>();
    executor.submit(
        () -> {
          try {
            var id = invoker.invoke(request);
            futureRequest.complete(id);
          } catch (RuntimeException ex) {
            futureRequest.completeExceptionally(ex);
          }
        });

    futureRequest
        .thenApply(
            r -> {
              log.info(
                  "webhook request message {} saved to database with id {} and status {}",
                  messageId,
                  r.getId(),
                  r.getStatus());
              ack.acknowledge();
              return r;
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

  @Scheduled(fixedRate = 5000, initialDelay = 5000L)
  void logKafkaOffsets() {
    var partitions =
        stats.keySet().stream()
            .map(key -> String.format("%s -> %d", key, stats.get(key)))
            .collect(Collectors.joining(", "));
    int activeCount = ((ThreadPoolExecutor) executor).getActiveCount();
    registry.gauge("svc_webhook_listner_executor_active_threads", activeCount);
    log.info("STATS: active thread {}, partition offsets: {} ", activeCount, partitions);
  }

  private void updateTopicStats(ConsumerRecord<String, Object> consumerRecord) {
    var key = String.format("%s-%d", consumerRecord.topic(), consumerRecord.partition());
    stats.put(key, consumerRecord.offset());
  }

  @PostConstruct
  private void initExecutor() {
    this.executor = Executors.newFixedThreadPool(size);
  }
}
