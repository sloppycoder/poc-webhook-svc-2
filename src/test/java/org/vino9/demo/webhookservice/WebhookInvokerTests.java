package org.vino9.demo.webhookservice;

import com.github.tomakehurst.wiremock.client.WireMock;
import de.mkammerer.wiremock.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.util.SocketUtils;
import org.vino9.demo.webhookservice.data.WebhookRequest;
import org.vino9.demo.webhookservice.data.WebhookRequestRepository;
import org.vino9.demo.webhookservice.webhook.RequestUtils;
import org.vino9.demo.webhookservice.webhook.WebhookInvoker;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class WebhookInvokerTests {
  @RegisterExtension
  WireMockExtension wireMock = new WireMockExtension(SocketUtils.findAvailableTcpPort());

  @SpyBean WebhookInvoker invoker;
  @Autowired private WebhookRequestRepository repository;

  @Value("${webhook.topic-pattern}")
  String testTopic;

  @Test
  void success_request_saved_to_database() {
    wireMock.stubFor(WireMock.post(anyUrl()).willReturn(WireMock.ok("OK")));
    String url =  wireMock.getBaseUri().resolve("/").toString();
    var request = RequestUtils.genDummyRequest(testTopic, url, "blah");
    var initCount = repository.count();

    invoker.invoke(request);

    assertEquals(WebhookRequest.Status.DONE, request.getStatus());
    assertEquals(initCount + 1, repository.count());
  }

  @Test
  void try_request_saved_to_database() {
    wireMock.stubFor(WireMock.post("/bad1").willReturn(WireMock.serverError().withBody("internal error")));
    wireMock.stubFor(WireMock.post("/bad2").willReturn(WireMock.unauthorized().withBody("access denied")));
    String url1 =  wireMock.getBaseUri().resolve("/bad1").toString();
    String url2 =  wireMock.getBaseUri().resolve("/bad1").toString();
    var request1 = RequestUtils.genDummyRequest(testTopic, url1, "blah");
    var request2 = RequestUtils.genDummyRequest(testTopic, url2, "blah");
    var initCount = repository.count();

    invoker.invoke(request1);
    invoker.invoke(request2);

    assertEquals(WebhookRequest.Status.RETRY, request1.getStatus());
    assertEquals(WebhookRequest.Status.RETRY, request2.getStatus());
    assertEquals(initCount + 2, repository.count());
  }

  @Test
  void bad_urls_fail_requests() {
    String url1 =  wireMock.getBaseUri().resolve("http://blah").toString(); // invalid DNS name
    String url2 =  wireMock.getBaseUri().resolve("httpx://google.com").toString(); // invalid protocol
    String url3 =  wireMock.getBaseUri().resolve("http:////bl/a").toString(); // malformed URL
    var request1 = RequestUtils.genDummyRequest(testTopic, url1, "blah");
    var request2 = RequestUtils.genDummyRequest(testTopic, url2, "blah");
    var request3 = RequestUtils.genDummyRequest(testTopic, url3, "blah");
    var initCount = repository.count();

    invoker.invoke(request1);
    invoker.invoke(request2);
    invoker.invoke(request3);

    assertEquals(WebhookRequest.Status.FAILED, request1.getStatus());
    assertEquals(WebhookRequest.Status.FAILED, request2.getStatus());
    assertEquals(WebhookRequest.Status.FAILED, request3.getStatus());
    assertEquals(initCount + 3, repository.count());
  }
}
