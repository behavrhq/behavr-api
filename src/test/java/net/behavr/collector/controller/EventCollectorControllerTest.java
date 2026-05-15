package net.behavr.collector.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import net.behavr.api.ApiApplication;
import net.behavr.collector.dto.EventBatchRequest;
import net.behavr.collector.dto.EventRequest;
import net.behavr.collector.kafka.EventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(classes = ApiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("local")
class EventCollectorControllerTest {

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private EventPublisher eventPublisher;

	@Test
	void validRequestReturns202() {
		EventRequest event = validEvent();
		EventBatchRequest batch = new EventBatchRequest();
		batch.setSiteId("site_123");
		batch.setSentAt(Instant.parse("2026-05-11T20:00:00Z"));
		batch.setEvents(List.of(event));

		webTestClient
				.post()
				.uri("/v1/events")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(batch)
				.exchange()
				.expectStatus()
				.isAccepted()
				.expectBody()
				.jsonPath("$.status")
				.isEqualTo("accepted")
				.jsonPath("$.accepted_events")
				.isEqualTo(1)
				.jsonPath("$.rejected_events")
				.isEqualTo(0);

		verify(eventPublisher, times(1)).publish(any());
	}

	@Test
	void emptyEventsReturns400() {
		EventBatchRequest batch = new EventBatchRequest();
		batch.setSiteId("site_123");
		batch.setSentAt(Instant.now());
		batch.setEvents(List.of());

		webTestClient
				.post()
				.uri("/v1/events")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(batch)
				.exchange()
				.expectStatus()
				.isBadRequest()
				.expectBody()
				.jsonPath("$.status")
				.isEqualTo("invalid_request");
	}

	@Test
	void missingSiteIdReturns400() {
		EventRequest event = validEvent();
		event.setSiteId("site_123");
		EventBatchRequest batch = new EventBatchRequest();
		batch.setSentAt(Instant.now());
		batch.setEvents(List.of(event));

		webTestClient
				.post()
				.uri("/v1/events")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(batch)
				.exchange()
				.expectStatus()
				.isBadRequest();
	}

	@Test
	void unknownEventTypeReturns400() {
		EventRequest event = validEvent();
		event.setEventType("unknown_type");
		EventBatchRequest batch = new EventBatchRequest();
		batch.setSiteId("site_123");
		batch.setSentAt(Instant.now());
		batch.setEvents(List.of(event));

		webTestClient
				.post()
				.uri("/v1/events")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(batch)
				.exchange()
				.expectStatus()
				.isBadRequest()
				.expectBody()
				.jsonPath("$.status")
				.isEqualTo("invalid_request");
	}

	@Test
	void eventSiteIdMismatchReturns400() {
		EventRequest event = validEvent();
		event.setSiteId("other_site");
		EventBatchRequest batch = new EventBatchRequest();
		batch.setSiteId("site_123");
		batch.setSentAt(Instant.now());
		batch.setEvents(List.of(event));

		webTestClient
				.post()
				.uri("/v1/events")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(batch)
				.exchange()
				.expectStatus()
				.isBadRequest()
				.expectBody()
				.jsonPath("$.status")
				.isEqualTo("invalid_request");
	}

	private static EventRequest validEvent() {
		EventRequest event = new EventRequest();
		event.setEventId("8f3dd1c7-0b7c-4baf-91a8-8f41d9d7d4c1");
		event.setEventType("search");
		event.setSiteId("site_123");
		event.setAnonymousId("anon_123");
		event.setSessionId("session_456");
		event.setOccurredAt(Instant.parse("2026-05-11T20:00:00Z"));
		event.setUrl("https://shop.example.com/search?q=hoodie");
		event.setPath("/search");
		event.setTitle("Search results");
		event.setReferrer("https://google.com");
		event.setUserAgent("Mozilla/5.0");
		event.setBrowserLanguage("en-US");
		event.setDeviceType("desktop");
		event.setSdkVersion("1.0.0");
		return event;
	}
}
