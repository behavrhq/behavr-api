package net.behavr.collector.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.behavr.collector.dto.EventBatchRequest;
import net.behavr.collector.dto.EventBatchResponse;
import net.behavr.collector.service.EventCollectorService;
import net.behavr.collector.support.ServerContextResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/v1")
@Validated
@RequiredArgsConstructor
public class EventCollectorController {

	private final EventCollectorService eventCollectorService;

	@PostMapping("/events")
	public Mono<ResponseEntity<EventBatchResponse>> collect(
			@Valid @RequestBody Mono<EventBatchRequest> body,
			@RequestHeader(value = "X-Behavr-Site-Key", required = false) String siteApiKey,
			ServerWebExchange exchange) {
		String requestId = UUID.randomUUID().toString();
		return body.flatMap(
				request -> Mono.fromCallable(() -> {
							var ctx = ServerContextResolver.resolve(exchange, requestId);
							return eventCollectorService.collect(request, ctx, siteApiKey);
						})
						.subscribeOn(Schedulers.boundedElastic())
						.map(response -> ResponseEntity.status(HttpStatus.ACCEPTED).body(response)));
	}
}
