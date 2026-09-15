package comp3011.assignment1.web;

import comp3011.assignment1.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

// builds the yaml error body in one place instead of every controller doing it.
// extends ResponseEntityExceptionHandler so spring's own errors (404, 405) keep their
// proper status - a plain @ExceptionHandler(Exception.class) on its own turns them all into 500s.
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(ShutdownInProgressException.class)
	public ResponseEntity<ErrorResponse> handleShutdownInProgress(HttpServletRequest request) {
		return build(HttpStatus.CONFLICT, "Graceful shutdown is already in progress.", request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleAnythingElse(Exception e, HttpServletRequest request) {
		log.error("unexpected error on {}", request.getRequestURI(), e);
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected server error occurred.", request);
	}

	private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest request) {
		ErrorResponse body = new ErrorResponse(
				Instant.now().truncatedTo(ChronoUnit.MILLIS),
				status.value(),
				status.getReasonPhrase(),
				message,
				request.getRequestURI());
		return ResponseEntity.status(status).body(body);
	}
}
