package comp3011.assignment1.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@Configuration
public class OpenAiConfig {

	private static final Logger log = LoggerFactory.getLogger(OpenAiConfig.class);

	// one RestClient for openai, set up once here so the service only deals with the call itself
	@Bean
	RestClient openAiClient(RestClient.Builder builder,
			@Value("${openai.base-url}") String baseUrl,
			@Value("${openai.api-key}") String apiKey) {

		// only log whether we got a key, never the key itself
		log.info("openai client -> {} (api key {})", baseUrl, apiKey.isBlank() ? "NOT set" : "set");

		return builder
				.baseUrl(baseUrl)
				.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
				.build();
	}
}
