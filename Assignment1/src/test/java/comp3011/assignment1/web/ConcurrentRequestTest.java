package comp3011.assignment1.web;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Load test for the non-functional requirement of handling more than 200 concurrent
 * blocking HTTP requests in one process.
 *
 * Sends 250 real HTTP uploads at the same time through the transcription controller and
 * checks that every one returns 200, that it finishes in a reasonable time rather than
 * queueing up, and that the token counters end on exactly the right total.
 *
 * Uses the stub profile so this measures our server and not OpenAI's response time, and
 * so the test costs nothing and doesn't need a key.
 *
 * What it proves: no deadlock, no thread starvation, no crash, and no lost counter
 * updates when 250 requests write to the same shared state at once.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("stub")
class ConcurrentRequestTest {

	private static final int REQUESTS = 250;

	@LocalServerPort
	private int port;

	@Test
	void handlesMoreThanTwoHundredSimultaneousUploads() throws Exception {
		HttpClient client = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(10))
				.build();

		// every request waits on this so they all go at once instead of trickling through
		CountDownLatch startSignal = new CountDownLatch(1);
		List<Future<Integer>> results = new ArrayList<>();

		long startedAt;
		try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
			for (int i = 0; i < REQUESTS; i++) {
				results.add(pool.submit(() -> {
					startSignal.await();
					HttpResponse<String> response =
							client.send(uploadRequest(), HttpResponse.BodyHandlers.ofString());
					return response.statusCode();
				}));
			}

			startedAt = System.currentTimeMillis();
			startSignal.countDown();

			for (Future<Integer> result : results) {
				// if the server deadlocked or starved this would time out instead of returning
				assertThat(result.get(60, TimeUnit.SECONDS)).isEqualTo(200);
			}
		}
		long took = System.currentTimeMillis() - startedAt;

		System.out.println("handled " + REQUESTS + " concurrent uploads in " + took + "ms");

		// generous, the point is that they overlap rather than queue one behind the other
		assertThat(took).isLessThan(30_000L);

		// the stub records 10 in / 5 out per call. an exact total means no update was lost
		// while 250 requests hit the counters at the same time.
		HttpResponse<String> stats = client.send(
				HttpRequest.newBuilder(URI.create(baseUrl() + "/api/v1/global/stats")).GET().build(),
				HttpResponse.BodyHandlers.ofString());

		assertThat(stats.body())
				.contains("\"inputTokens\":" + (REQUESTS * 10))
				.contains("\"outputTokens\":" + (REQUESTS * 5));
	}

	private String baseUrl() {
		return "http://localhost:" + port;
	}

	private HttpRequest uploadRequest() {
		String boundary = "----test" + System.nanoTime();
		return HttpRequest.newBuilder(URI.create(baseUrl() + "/api/v1/transcriptions"))
				.header("Content-Type", "multipart/form-data; boundary=" + boundary)
				.timeout(Duration.ofSeconds(60))
				.POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody(boundary)))
				.build();
	}

	// builds the multipart body by hand, same shape the browser sends
	private byte[] multipartBody(String boundary) {
		byte[] audio = new byte[2048];

		String head = "--" + boundary + "\r\n"
				+ "Content-Disposition: form-data; name=\"audio\"; filename=\"recording.webm\"\r\n"
				+ "Content-Type: audio/webm\r\n\r\n";
		String tail = "\r\n--" + boundary + "--\r\n";

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.writeBytes(head.getBytes(UTF_8));
		out.writeBytes(audio);
		out.writeBytes(tail.getBytes(UTF_8));
		return out.toByteArray();
	}
}
