package broke.fix.misc;

import java.time.InstantSource;

import javax.inject.Inject;

public class IncomingContext {
	public long transactTime;
	public CharSequence text;
	public CharSequence responseText;
	private InstantSource clock;

	@Inject
	public IncomingContext(InstantSource clock) {
		this.clock = clock;
	}

	public long getTime() {
		return clock.millis();
	}

	//TODO inject
	public long generateOrderID() {
		return System.nanoTime();
	}
}
