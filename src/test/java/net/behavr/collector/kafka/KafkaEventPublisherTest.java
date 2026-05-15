package net.behavr.collector.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import net.behavr.collector.config.BehavrProperties;
import net.behavr.collector.exception.KafkaPublishException;
import net.behavr.collector.model.CollectedEvent;
import net.behavr.collector.model.ServerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class KafkaEventPublisherTest {

	@Mock
	private KafkaTemplate<String, CollectedEvent> kafkaTemplate;

	private KafkaEventPublisher publisher;
	private final BehavrProperties behavrProperties = new BehavrProperties();
	private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

	@BeforeEach
	void setUp() {
		behavrProperties.getKafka().setTopic("behavr.events.raw");
		publisher = new KafkaEventPublisher(kafkaTemplate, behavrProperties, meterRegistry);
	}

	@Test
	void publishesWithSiteIdEventIdKey() {
		CollectedEvent event =
				new CollectedEvent(
						"e1",
						"search",
						"site_123",
						"a",
						"s",
						Instant.now(),
						Instant.now(),
						null,
						"https://x",
						null,
						null,
						null,
						null,
						null,
						null,
						null,
						Collections.emptyMap(),
						Collections.emptyMap(),
						new ServerContext(null, null, "r"));

		when(kafkaTemplate.send(eq("behavr.events.raw"), eq("site_123:e1"), eq(event)))
				.thenReturn(CompletableFuture.completedFuture(null));

		publisher.publish(event);

		ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
		verify(kafkaTemplate).send(eq("behavr.events.raw"), keyCaptor.capture(), eq(event));
		assertThat(keyCaptor.getValue()).isEqualTo("site_123:e1");
	}

	@Test
	void wrapsKafkaFailures() {
		CollectedEvent event =
				new CollectedEvent(
						"e1",
						"search",
						"site_123",
						"a",
						"s",
						Instant.now(),
						Instant.now(),
						null,
						"https://x",
						null,
						null,
						null,
						null,
						null,
						null,
						null,
						Collections.emptyMap(),
						Collections.emptyMap(),
						new ServerContext(null, null, "r"));

		CompletableFuture<org.springframework.kafka.support.SendResult<String, CollectedEvent>> failed =
				new CompletableFuture<>();
		failed.completeExceptionally(new RuntimeException("broker down"));
		when(kafkaTemplate.send(eq("behavr.events.raw"), eq("site_123:e1"), eq(event))).thenReturn(failed);

		assertThatThrownBy(() -> publisher.publish(event)).isInstanceOf(KafkaPublishException.class);
	}
}
