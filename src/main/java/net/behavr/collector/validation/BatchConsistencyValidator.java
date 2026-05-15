package net.behavr.collector.validation;

import java.util.List;
import net.behavr.collector.dto.EventRequest;
import net.behavr.collector.exception.InvalidEventTypeException;
import net.behavr.collector.exception.SiteIdMismatchException;

public final class BatchConsistencyValidator {

	private BatchConsistencyValidator() {}

	public static void validateEventAgainstBatch(String batchSiteId, EventRequest event, List<String> allowedTypes) {
		if (!batchSiteId.equals(event.getSiteId())) {
			throw new SiteIdMismatchException("event site_id must match batch site_id");
		}
		if (!allowedTypes.contains(event.getEventType())) {
			throw new InvalidEventTypeException("unknown event_type: " + event.getEventType());
		}
	}
}
