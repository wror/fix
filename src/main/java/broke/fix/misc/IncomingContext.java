package broke.fix.misc;

import java.time.InstantSource;
import java.util.function.Supplier;

import javax.inject.Inject;

public class IncomingContext {
	public long transactTime;
	public CharSequence text;
	public CharSequence responseText;
	private final InstantSource clock;
	private final Supplier<Long> orderIdGenerator;

	@Inject
	public IncomingContext(InstantSource clock, Supplier<Long> orderIdGenerator) {
		this.clock = clock;
		this.orderIdGenerator = orderIdGenerator;
	}

	public long getTime() {
		return clock.millis();
	}

	public long generateOrderID() {
		return orderIdGenerator.get();
	}
}
