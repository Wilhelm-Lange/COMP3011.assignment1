package comp3011.assignment1.service;

import comp3011.assignment1.dto.GlobalStatsResponse;
import org.springframework.stereotype.Service;

// token totals since the server started. in memory so they reset on restart.
@Service
public class TokenStatsService {

	private long inputTokens;
	private long outputTokens;

	// += is read-modify-write so it needs synchronizing (same race as the bank account in prac 1)
	public synchronized void record(long input, long output) {
		inputTokens += input;
		outputTokens += output;
	}

	// also synchronized, otherwise a reader could get one counter updated and the other not
	public synchronized GlobalStatsResponse currentStats() {
		return new GlobalStatsResponse(inputTokens, outputTokens);
	}
}
