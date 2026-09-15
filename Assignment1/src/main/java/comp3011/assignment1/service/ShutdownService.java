package comp3011.assignment1.service;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service
public class ShutdownService {

	private final ApplicationContext context;
	private final AtomicBoolean shutdownStarted = new AtomicBoolean(false);

	public ShutdownService(ApplicationContext context) {
		this.context = context;
	}

	// true if this call started the shutdown, false if one was already going.
	// compareAndSet checks and sets in one atomic step, so if two requests come in
	// together only one of them can get true. doing get() then set() would let both through.
	public boolean requestShutdown() {
		if (!shutdownStarted.compareAndSet(false, true)) {
			return false;
		}

		// do it on another thread, otherwise the server stops before this request
		// has sent its 202 back
		new Thread(() -> {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			System.exit(SpringApplication.exit(context, () -> 0));
		}).start();

		return true;
	}
}
