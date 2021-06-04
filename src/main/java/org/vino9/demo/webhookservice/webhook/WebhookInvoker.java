package org.vino9.demo.webhookservice.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.vino9.demo.webhookservice.data.WebhookRequest;
import org.vino9.demo.webhookservice.data.WebhookRequestRepository;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class WebhookInvoker {
  final WebhookRequestRepository repository;
  final ObjectMapper mapper;
  private final RestTemplate template;

  @Autowired
  public WebhookInvoker(
      WebhookRequestRepository repository, ObjectMapper mapper, RestTemplate template) {
    this.repository = repository;
    this.mapper = mapper;
    this.template = template;
  }

  public WebhookRequest invoke(WebhookRequest request) {
    callWebHookAndUpdateStatus(request);
    repository.save(request);
    return request;
  }

  private boolean callWebHookAndUpdateStatus(WebhookRequest request) {
    var payload = extractPayload(request);
    String url = payload.get("url");
    String message = payload.get("message");

    var headers = new HttpHeaders();
    headers.setContentType(MediaType.TEXT_PLAIN);
    var requestEntity = new HttpEntity<>(message, headers);
    try {
      var response = template.exchange(url, HttpMethod.POST, requestEntity, String.class);
      var status = response.getStatusCode();
      if (status.is2xxSuccessful()) { // is 1xx or 3xx considered successful?
        request.markDone();
        return true;
      }
    } catch (HttpServerErrorException e) {
      // HttpServerErrorException is thrown when server returns 4xx or 5xx errors
      request.markRetry();
    } catch (ResourceAccessException | IllegalArgumentException e) {
      // ResourceAccessException is thrown when DNS name can't be resolved
      // or protocol is not supported etc
      // IllegalArgumentException is thrown when the URL is malformed
      request.markFailed();
    } catch (Exception e) {
      // ResourceAccessException is thrown when DNS name can't be resolved
      // or protocol is not supported etc
      // IllegalArgumentException is thrown when the URL is malformed
      log.warn("failed to invoke webhook with exception {}", e);
      request.markFailed();
    }

    return false;
  }

  private Map<String, String> extractPayload(WebhookRequest request) {
    try {
      TypeReference<HashMap<String, String>> typeRef = new TypeReference<>() {};
      return mapper.readValue(request.getPayload(), typeRef);
    } catch (Exception e) {
      return null;
    }
  }
}
