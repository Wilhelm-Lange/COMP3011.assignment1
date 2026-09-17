package comp3011.assignment1.service;

import comp3011.assignment1.dto.OpenAiTranscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

// the real one. off when the stub profile is on, so only one of the two beans exists
@Profile("!stub")
@Service
public class OpenAiSpeechToTextService implements SpeechToTextService {

	private static final Logger log = LoggerFactory.getLogger(OpenAiSpeechToTextService.class);

	private final RestClient openAiClient;
	private final TokenStatsService tokenStats;
	private final String model;

	public OpenAiSpeechToTextService(RestClient openAiClient, TokenStatsService tokenStats,
			@Value("${openai.model}") String model) {
		this.openAiClient = openAiClient;
		this.tokenStats = tokenStats;
		this.model = model;
	}

	@Override
	public String transcribe(byte[] audio, String filename) {
		// openai works out the audio format from the filename extension, so it has to have one
		MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
		form.add("file", new ByteArrayResource(audio) {
			@Override
			public String getFilename() {
				return filename;
			}
		});
		form.add("model", model);
		// json is the only response_format gpt-4o-mini-transcribe supports
		form.add("response_format", "json");

		long startedAt = System.currentTimeMillis();
		OpenAiTranscription result;
		try {
			result = openAiClient.post()
					.uri("/audio/transcriptions")
					.contentType(MediaType.MULTIPART_FORM_DATA)
					.body(form)
					.retrieve()
					// deliberately don't include the response body in these messages,
					// it can echo request details back
					.onStatus(status -> status.value() == 401,
							(req, res) -> { throw new SttException("openai rejected the api key"); })
					.onStatus(HttpStatusCode::isError,
							(req, res) -> { throw new SttException("openai returned " + res.getStatusCode()); })
					.body(OpenAiTranscription.class);
		} catch (ResourceAccessException e) {
			// timeout or couldn't connect. we don't know if openai did the work or not,
			// but there's nothing to retry safely here so just report the failure.
			throw new SttException("could not reach openai", e);
		}

		long took = System.currentTimeMillis() - startedAt;

		if (result == null || result.text() == null) {
			throw new SttException("openai returned no text");
		}

		// usage is null for some models, so only count it when it's there
		if (result.usage() != null) {
			tokenStats.record(result.usage().inputTokens(), result.usage().outputTokens());
			log.info("transcribed {} bytes in {}ms, {} in / {} out tokens",
					audio.length, took, result.usage().inputTokens(), result.usage().outputTokens());
		} else {
			log.info("transcribed {} bytes in {}ms, no usage returned", audio.length, took);
		}

		return result.text();
	}
}
