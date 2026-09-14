package comp3011.assignment1.web;

import comp3011.assignment1.dto.UptimeResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin endpoints from the assignment YAML. */
@RestController
public class UptimeController {

	// Set once when Spring builds this bean during startup, so it is the process start time.
	// final + never reassigned means concurrent requests can read it safely without locking.
	private final Instant serverStart = Instant.now().truncatedTo(ChronoUnit.MILLIS);

	@GetMapping("/api/v1/admin/uptime")
	public UptimeResponse getServerUptime() {
		Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);

		// The YAML wants a floating point number of seconds (its example is 9000.5),
		// so divide millis rather than using toSeconds() which would throw away the fraction.
		double uptimeSeconds = Duration.between(serverStart, now).toMillis() / 1000.0;

		return new UptimeResponse(serverStart, now, uptimeSeconds);
	}
}
