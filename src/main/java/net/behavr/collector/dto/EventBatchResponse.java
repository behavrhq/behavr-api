package net.behavr.collector.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EventBatchResponse {

	private String status;

	@JsonProperty("accepted_events")
	private int acceptedEvents;

	@JsonProperty("rejected_events")
	private int rejectedEvents;
}
