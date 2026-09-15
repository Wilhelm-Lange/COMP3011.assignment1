package comp3011.assignment1.dto;

import java.time.Instant;

// the error body the yaml uses on every failing endpoint
public record ErrorResponse(Instant timestamp, int status, String error, String message, String path) {
}
