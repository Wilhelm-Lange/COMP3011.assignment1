package comp3011.assignment1.web;

import comp3011.assignment1.dto.ErrorResponse;
import comp3011.assignment1.dto.ShutdownResponse;
import comp3011.assignment1.service.ShutdownService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ShutdownController {

	private static final String PATH = "/api/v1/admin/shutdown";

	private final ShutdownService shutdownService;

	public ShutdownController(ShutdownService shutdownService) {
		this.shutdownService = shutdownService;
	}

	@PostMapping(PATH)
	public ResponseEntity<Object> shutdownServer() {
		if (!shutdownService.requestShutdown()) {
			ErrorResponse error = new ErrorResponse(
					Instant.now().truncatedTo(ChronoUnit.MILLIS),
					HttpStatus.CONFLICT.value(),
					"Conflict",
					"Graceful shutdown is already in progress.",
					PATH);
			return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
		}

		return ResponseEntity.accepted().body(new ShutdownResponse("Graceful shutdown requested."));
	}
}
