package net.behavr.collector.exception;

public class KafkaPublishException extends RuntimeException {

	public KafkaPublishException(String message, Throwable cause) {
		super(message, cause);
	}
}
