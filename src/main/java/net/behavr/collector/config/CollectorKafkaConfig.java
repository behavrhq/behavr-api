package net.behavr.collector.config;

import java.util.HashMap;
import java.util.Map;
import net.behavr.collector.model.CollectedEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

@Configuration
public class CollectorKafkaConfig {

	@Bean
	public ProducerFactory<String, CollectedEvent> collectedEventProducerFactory(KafkaProperties kafkaProperties) {
		Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties());
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
		// Configure JSON only via producer properties (e.g. spring.json.add.type.headers in application.yml).
		// Do not call setters on JacksonJsonSerializer here — that conflicts with merged config.
		return new DefaultKafkaProducerFactory<>(
				props, new StringSerializer(), new JacksonJsonSerializer<>());
	}

	@Bean
	public KafkaTemplate<String, CollectedEvent> collectedEventKafkaTemplate(
			ProducerFactory<String, CollectedEvent> collectedEventProducerFactory) {
		return new KafkaTemplate<>(collectedEventProducerFactory);
	}
}
