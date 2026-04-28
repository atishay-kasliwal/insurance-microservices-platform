package com.microservices.demo.kafka.streams.service.init;

import com.microservices.demo.kafka.streams.service.runner.impl.InsurancePolicyKafkaStreamsRunner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaStreamsInitializer {

    private final InsurancePolicyKafkaStreamsRunner streamsRunner;

    public KafkaStreamsInitializer(InsurancePolicyKafkaStreamsRunner streamsRunner) {
        this.streamsRunner = streamsRunner;
    }

    @EventListener
    public void onApplicationStarted(ApplicationStartedEvent event) {
        log.info("Starting Insurance Policy Kafka Streams topology");
        streamsRunner.start();
    }
}
