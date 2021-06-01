package org.vino9.demo.webhookservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {
    // now use Spring's default Kafka settings from applicatoin.yml
    // They can be overridden by defining your own beans here.
}
