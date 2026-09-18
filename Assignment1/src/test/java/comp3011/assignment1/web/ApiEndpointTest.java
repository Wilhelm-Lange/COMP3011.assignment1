package comp3011.assignment1.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Checks the endpoints against the shapes and status codes in the YAML.
 * Uses the stub profile so nothing calls the real OpenAI API.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("stub")
class ApiEndpointTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void uptimeHasTheThreeFieldsFromTheYaml() throws Exception {
		mockMvc.perform(get("/api/v1/admin/uptime"))
				.andExpect(status().isOk())
				// must be RFC 3339 strings, jackson writes epoch numbers by default
				.andExpect(jsonPath("$.utcServerStart").isString())
				.andExpect(jsonPath("$.utcNow").isString())
				.andExpect(jsonPath("$.serverUptimeSeconds").isNumber());
	}

	@Test
	void statsHasBothCounters() throws Exception {
		mockMvc.perform(get("/api/v1/global/stats"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.inputTokens").isNumber())
				.andExpect(jsonPath("$.outputTokens").isNumber());
	}

	@Test
	void uploadingAudioReturnsTheTranscript() throws Exception {
		MockMultipartFile audio =
				new MockMultipartFile("audio", "recording.webm", "audio/webm", new byte[] { 1, 2, 3, 4 });

		mockMvc.perform(multipart("/api/v1/transcriptions").file(audio))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.text").value("This is a stub transcription."));
	}

	@Test
	void emptyUploadIsRejected() throws Exception {
		MockMultipartFile empty =
				new MockMultipartFile("audio", "recording.webm", "audio/webm", new byte[0]);

		mockMvc.perform(multipart("/api/v1/transcriptions").file(empty))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}
}
