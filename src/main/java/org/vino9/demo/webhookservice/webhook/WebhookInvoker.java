package org.vino9.demo.webhookservice.webhook;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.vino9.demo.webhookservice.data.WebhookRequest;
import org.vino9.demo.webhookservice.data.WebhookRequestRepository;

import java.util.Random;

@Component
@Slf4j
public class WebhookInvoker {
  private RestTemplate template;
  private WebhookRequestRepository repository;
  private Random random = new Random();

  @Autowired
  public WebhookInvoker(WebhookRequestRepository repository) {
    this.repository = repository;
  }

  public Long invoke(WebhookRequest request) {
    try {
      int duration = random.nextInt(5000);
      log.info("sleeping for {} ms", duration);
      Thread.sleep(duration * 1L);
      if (duration > 2500) {
        return request.getId();
      }
    } catch (InterruptedException e) {
      // nothing we can do here.
    }
    throw new RuntimeException("invoke webhook failed");
  }
}
