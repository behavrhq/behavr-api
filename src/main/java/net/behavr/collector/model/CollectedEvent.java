package net.behavr.collector.model;

import java.time.Instant;
import java.util.Map;

public record CollectedEvent(
		String eventId,
		String eventType,
		String siteId,
		String anonymousId,
		String sessionId,
		Instant occurredAt,
		Instant receivedAt,
		Instant batchSentAt,
		String url,
		String path,
		String title,
		String referrer,
		String userAgent,
		String browserLanguage,
		String deviceType,
		String sdkVersion,
		Map<String, Object> utm,
		Map<String, Object> properties,
		ServerContext serverContext) {}
