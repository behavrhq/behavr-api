package net.behavr.collector.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;
import lombok.Data;

@Data
public class EventRequest {

	@NotBlank(message = "event_id is required")
	@JsonProperty("event_id")
	private String eventId;

	@NotBlank(message = "event_type is required")
	@JsonProperty("event_type")
	private String eventType;

	@NotBlank(message = "site_id is required")
	@JsonProperty("site_id")
	private String siteId;

	@NotBlank(message = "anonymous_id is required")
	@JsonProperty("anonymous_id")
	private String anonymousId;

	@NotBlank(message = "session_id is required")
	@JsonProperty("session_id")
	private String sessionId;

	@NotNull(message = "occurred_at is required")
	@JsonProperty("occurred_at")
	private Instant occurredAt;

	@NotBlank(message = "url is required")
	private String url;

	private String path;

	private String title;

	private String referrer;

	@JsonProperty("user_agent")
	private String userAgent;

	@JsonProperty("browser_language")
	private String browserLanguage;

	@JsonProperty("device_type")
	private String deviceType;

	@JsonProperty("sdk_version")
	private String sdkVersion;

	private Map<String, Object> utm;

	private Map<String, Object> properties;
}
