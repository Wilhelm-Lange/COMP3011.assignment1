package comp3011.assignment1.web;

import comp3011.assignment1.dto.TranscriptionResponse;
import comp3011.assignment1.service.SpeechToTextService;
import java.io.IOException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class TranscriptionController {

	private final SpeechToTextService speechToText;

	public TranscriptionController(SpeechToTextService speechToText) {
		this.speechToText = speechToText;
	}

	@PostMapping("/api/v1/transcriptions")
	public TranscriptionResponse transcribe(@RequestParam("audio") MultipartFile audio) throws IOException {
		if (audio.isEmpty()) {
			throw new BadUploadException("no audio was uploaded");
		}

		// the browser doesn't always set a filename but openai needs the extension
		// to know what format the audio is in
		String filename = audio.getOriginalFilename();
		if (filename == null || filename.isBlank()) {
			filename = "recording.webm";
		}

		return new TranscriptionResponse(speechToText.transcribe(audio.getBytes(), filename));
	}
}
