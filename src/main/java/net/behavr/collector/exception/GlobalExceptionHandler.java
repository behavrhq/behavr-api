package net.behavr.collector.exception;

import jakarta.validation.ConstraintViolationException;
import net.behavr.collector.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(WebExchangeBindException.class)
	public Mono<ResponseEntity<ErrorResponse>> handleBind(WebExchangeBindException ex) {
		String message =
				ex.getBindingResult().getAllErrors().stream()
						.findFirst()
						.map(err -> err.getDefaultMessage() != null ? err.getDefaultMessage() : err.getCode())
						.orElse("invalid request");
		return Mono.just(
				ResponseEntity.badRequest().body(ErrorResponse.invalidRequest(message)));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public Mono<ResponseEntity<ErrorResponse>> handleConstraint(ConstraintViolationException ex) {
		String message = ex.getConstraintViolations().stream()
				.findFirst()
				.map(v -> v.getPropertyPath() + " " + v.getMessage())
				.orElse(ex.getMessage());
		return Mono.just(
				ResponseEntity.badRequest().body(ErrorResponse.invalidRequest(message)));
	}

	@ExceptionHandler(ServerWebInputException.class)
	public Mono<ResponseEntity<ErrorResponse>> handleServerWebInput(ServerWebInputException ex) {
		return Mono.just(
				ResponseEntity.badRequest()
						.body(ErrorResponse.invalidRequest(ex.getReason() != null ? ex.getReason() : "invalid request")));
	}

	@ExceptionHandler(DecodingException.class)
	public Mono<ResponseEntity<ErrorResponse>> handleDecoding(DecodingException ex) {
		return Mono.just(
				ResponseEntity.badRequest()
						.body(ErrorResponse.invalidRequest("malformed JSON")));
	}

	@ExceptionHandler(InvalidEventTypeException.class)
	public Mono<ResponseEntity<ErrorResponse>> handleInvalidEventType(InvalidEventTypeException ex) {
		return Mono.just(
				ResponseEntity.badRequest().body(ErrorResponse.invalidRequest(ex.getMessage())));
	}

	@ExceptionHandler(SiteIdMismatchException.class)
	public Mono<ResponseEntity<ErrorResponse>> handleSiteMismatch(SiteIdMismatchException ex) {
		return Mono.just(
				ResponseEntity.badRequest().body(ErrorResponse.invalidRequest(ex.getMessage())));
	}

	@ExceptionHandler(BatchSizeExceededException.class)
	public Mono<ResponseEntity<ErrorResponse>> handleBatchSize(BatchSizeExceededException ex) {
		return Mono.just(
				ResponseEntity.badRequest().body(ErrorResponse.invalidRequest(ex.getMessage())));
	}

	@ExceptionHandler(SiteApiKeyException.class)
	public Mono<ResponseEntity<ErrorResponse>> handleSiteKey(SiteApiKeyException ex) {
		return Mono.just(
				ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse.error(ex.getMessage())));
	}

	@ExceptionHandler(KafkaPublishException.class)
	public Mono<ResponseEntity<ErrorResponse>> handleKafka(KafkaPublishException ex) {
		return Mono.just(
				ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
						.body(ErrorResponse.error("Event ingestion temporarily unavailable")));
	}

	@ExceptionHandler(Exception.class)
	public Mono<ResponseEntity<ErrorResponse>> handleGeneric(Exception ex) {
		log.error("Unhandled error", ex);
		return Mono.just(
				ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
						.body(ErrorResponse.error("Internal server error")));
	}
}
