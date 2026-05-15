package net.behavr.collector.kafka;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.behavr.collector.config.BehavrProperties;
import net.behavr.collector.exception.KafkaPublishException;
import net.behavr.collector.model.CollectedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaEventPublisher implements EventPublisher {

	private final KafkaTemplate<String, CollectedEvent> kafkaTemplate;
	private final BehavrProperties behavrProperties;
	private final MeterRegistry meterRegistry;

	@Override
	public void publish(CollectedEvent event) {
		String key = event.siteId() + ":" + event.eventId();
		String topic = behavrProperties.getKafka().getTopic();
		try {
			kafkaTemplate.send(topic, key, event).get();
			log.debug("Published event to Kafka site_id={} event_id={}", event.siteId(), event.eventId());
		} catch (Exception e) {
			log.error(
					"Kafka publish failed site_id={} event_type={} event_id={}: {}",
					event.siteId(),
					event.eventType(),
					event.eventId(),
					e.toString());
			meterRegistry
					.counter(
							"behavr_kafka_publish_errors_total",
							Tags.of("site_id", event.siteId(), "event_type", event.eventType()))
					.increment();
			throw new KafkaPublishException("Kafka publish failed", e);
		}
	}
}
