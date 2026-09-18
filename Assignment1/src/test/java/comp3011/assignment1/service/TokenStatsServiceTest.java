package comp3011.assignment1.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Race condition test for the token counters.
 *
 * inputTokens += n is a read, an add and a write. If that isn't synchronized then two
 * threads can read the same value, both add to it, and one update gets lost - the same
 * bug as the bank account in Practical 1. This test runs a lot of updates at once and
 * checks the total is exactly right, because a lost update shows up as a total that is
 * too low.
 *
 * Without synchronized on record() this test fails. It passes with it.
 */
class TokenStatsServiceTest {

	@Test
	void doesNotLoseUpdatesWhenManyThreadsRecordAtOnce() throws Exception {
		TokenStatsService stats = new TokenStatsService();

		int threads = 250;
		int perThread = 100;

		// all threads wait on the latch so they start together and actually overlap.
		// without this they'd mostly run one after another and the race wouldn't show.
		CountDownLatch startSignal = new CountDownLatch(1);
		CountDownLatch finished = new CountDownLatch(threads);

		try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
			for (int i = 0; i < threads; i++) {
				pool.submit(() -> {
					startSignal.await();
					for (int n = 0; n < perThread; n++) {
						stats.record(2, 1);
					}
					finished.countDown();
					return null;
				});
			}

			startSignal.countDown();
			assertThat(finished.await(30, TimeUnit.SECONDS)).isTrue();
		}

		// 250 threads x 100 updates x 2 in / 1 out
		assertThat(stats.currentStats().inputTokens()).isEqualTo(250L * 100 * 2);
		assertThat(stats.currentStats().outputTokens()).isEqualTo(250L * 100 * 1);
	}

	@Test
	void readsBothCountersAsAConsistentPair() throws Exception {
		TokenStatsService stats = new TokenStatsService();

		// one thread keeps recording while another keeps reading. every record adds
		// 2 in and 1 out, so a consistent read always has inputTokens == 2 * outputTokens.
		// if the getter wasn't synchronized a reader could catch one counter updated and
		// the other not, and see a pair that was never actually true.
		Thread writer = Thread.ofVirtual().start(() -> {
			for (int i = 0; i < 50_000; i++) {
				stats.record(2, 1);
			}
		});

		for (int i = 0; i < 50_000; i++) {
			var snapshot = stats.currentStats();
			assertThat(snapshot.inputTokens()).isEqualTo(snapshot.outputTokens() * 2);
		}

		writer.join();
	}
}
