package net.behavr.collector.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class CollectorKafkaTopicsConfig {

	@Bean
	@ConditionalOnProperty(
			prefix = "behavr.kafka",
			name = "ensure-topic",
			havingValue = "true",
			matchIfMissing = true)
	public NewTopic behavrRawEventsTopic(BehavrProperties behavrProperties) {
		var kafka = behavrProperties.getKafka();
		return TopicBuilder.name(kafka.getTopic())
				.partitions(kafka.getTopicPartitions())
				.replicas(kafka.getTopicReplicationFactor())
				.build();
	}
}
