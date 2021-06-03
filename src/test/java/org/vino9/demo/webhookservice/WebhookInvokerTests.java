package org.vino9.demo.webhookservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.vino9.demo.webhookservice.data.WebhookRequestRepository;
import org.vino9.demo.webhookservice.webhook.RequestUtils;
import org.vino9.demo.webhookservice.webhook.WebhookInvoker;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class WebhookInvokerTests {

  @Autowired private WebhookRequestRepository repository;

  @SpyBean WebhookInvoker invoker;

  @Test
  void invoke_saves_request_to_database() {
    var request = RequestUtils.genDummyRequest("client_id", "http://google.com", "blah");
    var initCount = repository.count();

    invoker.invoke(request);

    assertEquals(initCount + 1, repository.count());
  }
}
