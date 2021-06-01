package org.vino9.demo.webhookservice.data;

import org.springframework.data.repository.CrudRepository;

public interface WebhookRequestRepository extends CrudRepository<WebhookRequest, Long> {
}
