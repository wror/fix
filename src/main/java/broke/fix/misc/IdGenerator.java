package broke.fix.misc;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static java.time.LocalTime.MIDNIGHT;
import static java.time.LocalDate.now;
import static java.time.LocalDateTime.of;
import static java.time.ZoneOffset.systemDefault;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.apache.logging.log4j.util.Unbox.box;
import static java.lang.Long.MAX_VALUE;

public class IdGenerator {
	private final static Logger log = LogManager.getLogger();
	private long lastID;
	private long midnight = of(now(), MIDNIGHT).plusDays(1).atZone(systemDefault()).toInstant().toEpochMilli();
	private long lastLoggedID;


	public CharSequence getClOrdID() {
		return Long.toHexString(getOrderID());
	}

	public long getOrderID() {
		long now = System.currentTimeMillis();
		if (now > lastID) {
			lastID = now;
			return lastID;
		} else {
			logOfTooManyIDs();
			lastID += 1;
		}
		return lastID;
	}

	private void logOfTooManyIDs() {
		if (lastID - lastLoggedID > MINUTES.toMillis(10)) { //suppress logging this more than once every 10 minutes
			log.info("Allocating more than a thousand orderIDs per second (e.g. {}), please be careful if you're planning to discard state and restart", box(lastID));
			lastLoggedID = lastID;
		}
		if (lastID > midnight && lastLoggedID < midnight) { //suppress logging this more than once
			log.warn("Allocated an orderID extending into the next calendar day (e.g. {}), please investigate and ensure that orderIDs will not be reused after restart", box(lastID));
			lastLoggedID = lastID;
		}
		if (lastID == MAX_VALUE) {
			log.warn("Allocated an orderID past maximium supported value ({}). Wrapping around!", box(MAX_VALUE));
		}
	}
}
