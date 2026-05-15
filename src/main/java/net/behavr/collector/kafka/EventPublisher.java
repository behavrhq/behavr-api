package net.behavr.collector.kafka;

import net.behavr.collector.model.CollectedEvent;

@FunctionalInterface
public interface EventPublisher {

	void publish(CollectedEvent event);
}
