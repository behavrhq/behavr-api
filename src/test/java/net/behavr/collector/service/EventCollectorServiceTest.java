package net.behavr.collector.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.behavr.collector.config.BehavrCollectorProperties;
import net.behavr.collector.config.BehavrKafkaProperties;
import net.behavr.collector.config.BehavrProperties;
import net.behavr.collector.dto.EventBatchRequest;
import net.behavr.collector.dto.EventBatchResponse;
import net.behavr.collector.dto.EventRequest;
import net.behavr.collector.exception.BatchSizeExceededException;
import net.behavr.collector.kafka.EventPublisher;
import net.behavr.collector.model.CollectedEvent;
import net.behavr.collector.model.ServerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

@ExtendWith(MockitoExtension.class)
class EventCollectorServiceTest {

	@Mock
	private EventPublisher eventPublisher;

	@Mock
	private Environment environment;

	private BehavrProperties behavrProperties;
	private EventCollectorService service;
	private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

	@BeforeEach
	void setUp() {
		behavrProperties = new BehavrProperties();
		behavrProperties.setCollector(new BehavrCollectorProperties());
		behavrProperties.setKafka(new BehavrKafkaProperties());
		behavrProperties.getSites().put("site_123", "test_secret_key");
		when(environment.matchesProfiles("local")).thenReturn(true);
		service = new EventCollectorService(behavrProperties, eventPublisher, meterRegistry, environment);
	}

	@Test
	void transformsRequestAddsReceivedAtAndRequestIdAndPublishes() {
		Instant sentAt = Instant.parse("2026-05-11T20:00:00Z");
		EventBatchRequest batch = new EventBatchRequest();
		batch.setSiteId("site_123");
		batch.setSentAt(sentAt);
		batch.setEvents(List.of(sampleEvent()));

		ServerContext ctx = new ServerContext("203.0.113.1", "JUnit", "req-1");

		EventBatchResponse response = service.collect(batch, ctx, null);

		assertThat(response.getStatus()).isEqualTo("accepted");
		assertThat(response.getAcceptedEvents()).isEqualTo(1);
		assertThat(response.getRejectedEvents()).isEqualTo(0);

		ArgumentCaptor<CollectedEvent> captor = ArgumentCaptor.forClass(CollectedEvent.class);
		verify(eventPublisher).publish(captor.capture());
		CollectedEvent published = captor.getValue();
		assertThat(published.receivedAt()).isNotNull();
		assertThat(published.batchSentAt()).isEqualTo(sentAt);
		assertThat(published.serverContext().requestId()).isEqualTo("req-1");
		assertThat(published.serverContext().ipAddress()).isEqualTo("203.0.113.1");
		assertThat(published.eventType()).isEqualTo("search");
	}

	@Test
	void rejectsBatchExceedingMaxSize() {
		behavrProperties.getCollector().setMaxBatchSize(2);
		EventBatchRequest batch = new EventBatchRequest();
		batch.setSiteId("site_123");
		batch.setSentAt(Instant.now());
		List<EventRequest> events = new ArrayList<>();
		events.add(sampleEvent());
		events.add(sampleEvent());
		events.add(sampleEvent());
		batch.setEvents(events);

		ServerContext ctx = new ServerContext(null, null, "r");

		assertThatThrownBy(() -> service.collect(batch, ctx, null))
				.isInstanceOf(BatchSizeExceededException.class);
	}

	@Test
	void publishUsesSiteAndEventIdInKafkaKeyConvention() {
		EventBatchRequest batch = new EventBatchRequest();
		batch.setSiteId("site_123");
		batch.setSentAt(Instant.now());
		EventRequest ev = sampleEvent();
		ev.setEventId("evt-99");
		batch.setEvents(List.of(ev));

		service.collect(batch, new ServerContext(null, null, "rid"), null);

		verify(eventPublisher)
				.publish(
						argThat(
								e -> "site_123".equals(e.siteId())
										&& "evt-99".equals(e.eventId())
										&& ("site_123" + ":" + "evt-99").equals(e.siteId() + ":" + e.eventId())));
	}

	private static EventRequest sampleEvent() {
		EventRequest event = new EventRequest();
		event.setEventId("8f3dd1c7-0b7c-4baf-91a8-8f41d9d7d4c1");
		event.setEventType("search");
		event.setSiteId("site_123");
		event.setAnonymousId("anon");
		event.setSessionId("sess");
		event.setOccurredAt(Instant.parse("2026-05-11T20:00:00Z"));
		event.setUrl("https://example.com");
		event.setUtm(Map.of());
		event.setProperties(Map.of("q", "x"));
		return event;
	}
}
