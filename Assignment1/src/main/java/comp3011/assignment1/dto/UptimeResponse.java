package comp3011.assignment1.dto;

import java.time.Instant;

// matches the UptimeResponse schema in the yaml
public record UptimeResponse(Instant utcServerStart, Instant utcNow, double serverUptimeSeconds) {
}
