package comp3011.assignment1.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

// fake stt for local testing and for the tests, so nothing calls the real
// openai api (no key needed, no cost, and it returns instantly).
// run with --spring.profiles.active=stub
@Profile("stub")
@Service
public class StubSpeechToTextService implements SpeechToTextService {

	private static final Logger log = LoggerFactory.getLogger(StubSpeechToTextService.class);

	private final TokenStatsService tokenStats;

	public StubSpeechToTextService(TokenStatsService tokenStats) {
		this.tokenStats = tokenStats;
	}

	@Override
	public String transcribe(byte[] audio, String filename) {
		// make up some token usage so the stats endpoint has something to report
		tokenStats.record(10, 5);
		log.info("stub transcribed {} bytes from {}", audio.length, filename);
		return "This is a stub transcription.";
	}
}
