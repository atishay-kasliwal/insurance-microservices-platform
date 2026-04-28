package com.microservices.demo.kafka.streams.service.runner.impl;

import com.microservices.demo.config.KafkaConfigData;
import com.microservices.demo.kafka.avro.model.TwitterAvroModel;
import com.microservices.demo.kafka.streams.service.runner.StreamsRunner;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.Properties;

@Slf4j
@Component
public class InsurancePolicyKafkaStreamsRunner implements StreamsRunner<String, Long> {

    public static final String POLICY_COUNT_STORE = "insurance-policy-counts";
    private static final String COUNTS_TOPIC = "insurance-policy-counts-topic";

    private final KafkaConfigData kafkaConfigData;
    private final Properties streamsProperties;
    private final Map<String, String> serdeConfig;

    private KafkaStreams kafkaStreams;

    public InsurancePolicyKafkaStreamsRunner(KafkaConfigData kafkaConfigData,
                                             Properties streamsProperties,
                                             Map<String, String> serdeConfig) {
        this.kafkaConfigData = kafkaConfigData;
        this.streamsProperties = streamsProperties;
        this.serdeConfig = serdeConfig;
    }

    @Override
    public void start() {
        StreamsBuilder builder = new StreamsBuilder();

        SpecificAvroSerde<TwitterAvroModel> avroSerde = new SpecificAvroSerde<>();
        avroSerde.configure(serdeConfig, false);

        KStream<Long, TwitterAvroModel> insuranceStream = builder.stream(
            kafkaConfigData.getTopicName(),
            Consumed.with(Serdes.Long(), avroSerde)
        );

        // Extract policy type from the message text (first word acts as policy category)
        // and count occurrences — represents insurance policy type frequency analytics
        insuranceStream
            .map((key, value) -> {
                String policyType = extractPolicyType(value.getText());
                return KeyValue.pair(policyType, value);
            })
            .groupByKey(
                org.apache.kafka.streams.kstream.Grouped.with(Serdes.String(), avroSerde)
            )
            .count(Materialized.as(POLICY_COUNT_STORE))
            .toStream()
            .to(COUNTS_TOPIC, Produced.with(Serdes.String(), Serdes.Long()));

        kafkaStreams = new KafkaStreams(builder.build(), streamsProperties);
        kafkaStreams.setUncaughtExceptionHandler((thread, throwable) -> {
            log.error("Insurance streams error on thread {}", thread.getName(), throwable);
        });
        kafkaStreams.start();
        log.info("Insurance Policy Kafka Streams started — listening on topic '{}'",
            kafkaConfigData.getTopicName());
    }

    @Override
    public Long getValueByKey(String policyType) {
        if (kafkaStreams == null || kafkaStreams.state() != KafkaStreams.State.RUNNING) {
            log.warn("Kafka Streams not running yet, state: {}",
                kafkaStreams != null ? kafkaStreams.state() : "null");
            return 0L;
        }
        ReadOnlyKeyValueStore<String, Long> store = kafkaStreams.store(
            StoreQueryParameters.fromNameAndType(POLICY_COUNT_STORE, QueryableStoreTypes.keyValueStore())
        );
        Long count = store.get(policyType);
        return count != null ? count : 0L;
    }

    private String extractPolicyType(String text) {
        if (text == null || text.isBlank()) {
            return "UNKNOWN";
        }
        String firstWord = text.trim().split("\\s+")[0].toUpperCase().replaceAll("[^A-Z]", "");
        return firstWord.isEmpty() ? "UNKNOWN" : firstWord;
    }

    @PreDestroy
    public void close() {
        if (kafkaStreams != null) {
            kafkaStreams.close();
            log.info("Insurance Policy Kafka Streams closed");
        }
    }
}
