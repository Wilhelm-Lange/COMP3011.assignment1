package comp3011.assignment1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// what openai sends back from /audio/transcriptions.
// only the fields we actually use, jackson ignores the rest.
public record OpenAiTranscription(String text, Usage usage) {

	public record Usage(
			@JsonProperty("input_tokens") long inputTokens,
			@JsonProperty("output_tokens") long outputTokens) {
	}
}
