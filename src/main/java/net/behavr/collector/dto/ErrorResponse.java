package net.behavr.collector.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorResponse {

	private String status;

	private String message;

	public static ErrorResponse invalidRequest(String message) {
		return new ErrorResponse("invalid_request", message);
	}

	public static ErrorResponse error(String message) {
		return new ErrorResponse("error", message);
	}
}
