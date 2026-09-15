package comp3011.assignment1.web;

import comp3011.assignment1.dto.GlobalStatsResponse;
import comp3011.assignment1.service.TokenStatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GlobalStatsController {

	private final TokenStatsService tokenStatsService;

	public GlobalStatsController(TokenStatsService tokenStatsService) {
		this.tokenStatsService = tokenStatsService;
	}

	@GetMapping("/api/v1/global/stats")
	public GlobalStatsResponse getGlobalStats() {
		return tokenStatsService.currentStats();
	}
}
