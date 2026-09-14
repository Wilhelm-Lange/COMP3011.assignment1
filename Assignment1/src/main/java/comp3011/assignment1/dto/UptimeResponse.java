package comp3011.assignment1.dto;

import java.time.Instant;

/**
 * Response body for GET /api/v1/admin/uptime, matching the UptimeResponse schema in the YAML.
 * A record is used because this is just an immutable value that gets serialised to JSON.
 */
public record UptimeResponse(Instant utcServerStart, Instant utcNow, double serverUptimeSeconds) {
}
