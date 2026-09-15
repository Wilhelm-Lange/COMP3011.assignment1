package comp3011.assignment1.web;

import comp3011.assignment1.dto.ShutdownResponse;
import comp3011.assignment1.service.ShutdownService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ShutdownController {

	private final ShutdownService shutdownService;

	public ShutdownController(ShutdownService shutdownService) {
		this.shutdownService = shutdownService;
	}

	@PostMapping("/api/v1/admin/shutdown")
	public ResponseEntity<ShutdownResponse> shutdownServer() {
		if (!shutdownService.requestShutdown()) {
			throw new ShutdownInProgressException();
		}
		return ResponseEntity.accepted().body(new ShutdownResponse("Graceful shutdown requested."));
	}
}
