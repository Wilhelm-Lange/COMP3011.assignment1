package comp3011.assignment1.web;

import comp3011.assignment1.dto.UptimeResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UptimeController {

	// set once when spring makes this bean, so it's the server start time.
	// never changes after that so concurrent requests can read it without locking.
	private final Instant serverStart = Instant.now().truncatedTo(ChronoUnit.MILLIS);

	@GetMapping("/api/v1/admin/uptime")
	public UptimeResponse getServerUptime() {
		Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);

		// yaml wants a decimal (example is 9000.5), toSeconds() would chop the .5 off
		double uptimeSeconds = Duration.between(serverStart, now).toMillis() / 1000.0;

		return new UptimeResponse(serverStart, now, uptimeSeconds);
	}
}
