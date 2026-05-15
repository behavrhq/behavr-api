package net.behavr.collector.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.behavr.collector.config.BehavrProperties;
import net.behavr.collector.dto.EventBatchRequest;
import net.behavr.collector.dto.EventBatchResponse;
import net.behavr.collector.dto.EventRequest;
import net.behavr.collector.exception.BatchSizeExceededException;
import net.behavr.collector.exception.InvalidEventTypeException;
import net.behavr.collector.exception.SiteApiKeyException;
import net.behavr.collector.exception.SiteIdMismatchException;
import net.behavr.collector.kafka.EventPublisher;
import net.behavr.collector.model.CollectedEvent;
import net.behavr.collector.model.ServerContext;
import net.behavr.collector.validation.BatchConsistencyValidator;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventCollectorService {

	private final BehavrProperties behavrProperties;
	private final EventPublisher eventPublisher;
	private final MeterRegistry meterRegistry;
	private final Environment environment;

	public EventBatchResponse collect(EventBatchRequest request, ServerContext serverContext, String siteApiKey) {
		validateSiteApiKey(request.getSiteId(), siteApiKey);
		validateBatchSize(request.getEvents());

		int received = request.getEvents().size();
		log.info(
				"Event batch received site_id={} event_count={} request_id={}",
				request.getSiteId(),
				received,
				serverContext.requestId());

		List<String> allowed = behavrProperties.getCollector().getAllowedEventTypes();
		for (EventRequest event : request.getEvents()) {
			try {
				BatchConsistencyValidator.validateEventAgainstBatch(request.getSiteId(), event, allowed);
			} catch (SiteIdMismatchException ex) {
				log.warn(
						"Validation failed site_id mismatch batch_site_id={} event_site_id={} request_id={}",
						request.getSiteId(),
						event.getSiteId(),
						serverContext.requestId());
				meterRegistry
						.counter(
								"behavr_events_rejected_total",
								Tags.of("site_id", request.getSiteId(), "event_type", "site_mismatch"))
						.increment();
				throw ex;
			} catch (InvalidEventTypeException ex) {
				log.warn(
						"Validation failed unknown event_type={} site_id={} request_id={}",
						event.getEventType(),
						request.getSiteId(),
						serverContext.requestId());
				meterRegistry
						.counter(
								"behavr_events_rejected_total",
								Tags.of("site_id", request.getSiteId(), "event_type", "unknown_type"))
						.increment();
				throw ex;
			}
		}

		Instant receivedAt = Instant.now();
		for (EventRequest event : request.getEvents()) {
			meterRegistry
					.counter(
							"behavr_events_received_total",
							Tags.of("site_id", event.getSiteId(), "event_type", event.getEventType()))
					.increment();
			CollectedEvent collected = toCollectedEvent(request, event, serverContext, receivedAt);
			eventPublisher.publish(collected);
			meterRegistry
					.counter(
							"behavr_events_accepted_total",
							Tags.of("site_id", event.getSiteId(), "event_type", event.getEventType()))
					.increment();
		}

		return new EventBatchResponse("accepted", received, 0);
	}

	private void validateSiteApiKey(String siteId, String providedKey) {
		if (environment.matchesProfiles("local")) {
			return;
		}
		if (providedKey == null || providedKey.isBlank()) {
			throw new SiteApiKeyException("X-Behavr-Site-Key header is required");
		}
		String expected = behavrProperties.getSites().get(siteId);
		if (expected == null) {
			throw new SiteApiKeyException("Unknown site_id or site not configured for API keys");
		}
		if (!constantTimeEquals(providedKey, expected)) {
			throw new SiteApiKeyException("Invalid site API key");
		}
	}

	private static boolean constantTimeEquals(String a, String b) {
		byte[] aa = a.getBytes(StandardCharsets.UTF_8);
		byte[] bb = b.getBytes(StandardCharsets.UTF_8);
		return MessageDigest.isEqual(aa, bb);
	}

	private void validateBatchSize(List<EventRequest> events) {
		int max = behavrProperties.getCollector().getMaxBatchSize();
		if (events.size() > max) {
			log.warn("Validation failed batch size {} exceeds max {}", events.size(), max);
			throw new BatchSizeExceededException("events size must not exceed " + max);
		}
	}

	private static CollectedEvent toCollectedEvent(
			EventBatchRequest batch, EventRequest event, ServerContext serverContext, Instant receivedAt) {
		Map<String, Object> utm = event.getUtm() != null ? event.getUtm() : Collections.emptyMap();
		Map<String, Object> properties =
				event.getProperties() != null ? event.getProperties() : Collections.emptyMap();
		return new CollectedEvent(
				event.getEventId(),
				event.getEventType(),
				event.getSiteId(),
				event.getAnonymousId(),
				event.getSessionId(),
				event.getOccurredAt(),
				receivedAt,
				batch.getSentAt(),
				event.getUrl(),
				event.getPath(),
				event.getTitle(),
				event.getReferrer(),
				event.getUserAgent(),
				event.getBrowserLanguage(),
				event.getDeviceType(),
				event.getSdkVersion(),
				utm,
				properties,
				serverContext);
	}
}
