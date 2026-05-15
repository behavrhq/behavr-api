package net.behavr.collector.config;

import lombok.Data;

@Data
public class BehavrKafkaProperties {

	private String topic = "behavr.events.raw";

	/**
	 * When true, register a {@link org.apache.kafka.clients.admin.NewTopic} bean so Spring creates the topic at
	 * startup (requires broker/API key ACLs that allow create). Disable if topics are managed only by IaC.
	 */
	private boolean ensureTopic = true;

	/** Partition count for {@link #topic} when {@link #ensureTopic} is true. */
	private int topicPartitions = 1;

	/**
	 * Replication factor when {@link #ensureTopic} is true. Use 1 for single-broker local Docker; Confluent Cloud
	 * typically requires 3.
	 */
	private int topicReplicationFactor = 1;
}
