package com.microservices.demo.kafka.streams.service.api;

import com.microservices.demo.kafka.streams.service.runner.impl.InsurancePolicyKafkaStreamsRunner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/kafka-streams/insurance")
public class KafkaStreamsController {

    private final InsurancePolicyKafkaStreamsRunner streamsRunner;

    public KafkaStreamsController(InsurancePolicyKafkaStreamsRunner streamsRunner) {
        this.streamsRunner = streamsRunner;
    }

    @GetMapping("/policy-counts/{policyType}")
    public ResponseEntity<Long> getPolicyCountByType(@PathVariable String policyType) {
        Long count = streamsRunner.getValueByKey(policyType.toUpperCase());
        log.info("Policy count query for type '{}': {}", policyType, count);
        return ResponseEntity.ok(count);
    }
}
