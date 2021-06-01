package org.vino9.demo.webhookservice.data;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "webhook_requests")
public class WebhookRequest {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private long id;
  private String messageId;
  private String messageType;
  private String clientId;
  private String payload;
  @Builder.Default
  private Status status = Status.NEW;
  @Builder.Default
  private int retries = 0;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public WebhookRequest markDone() {
    setStatus(Status.DONE);
    return this;
  }

  public WebhookRequest markFailed() {
    setStatus(Status.FAILED);
    return this;
  }

  private void setStatus(Status newStatus) {
    LocalDateTime now = LocalDateTime.now();
    this.status = newStatus;
    this.updatedAt = now;
  }

  public enum Status {
    NEW,
    LOCKED,
    DONE,
    RETRY,
    FAILED
  }
}