package net.behavr.collector.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.Instant;
import java.util.List;
import lombok.Data;

@Data
public class EventBatchRequest {

	@NotBlank(message = "site_id is required")
	@JsonProperty("site_id")
	private String siteId;

	@JsonProperty("sent_at")
	private Instant sentAt;

	@NotEmpty(message = "events must not be empty")
	@Valid
	private List<EventRequest> events;
}
