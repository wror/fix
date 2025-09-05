package broke.fix.misc;

public class IdGenerator {
	long lastID;

	public CharSequence getClOrdID() {
		return Long.toHexString(getOrderID());
	}

	public long getOrderID() {
		long now = System.currentTimeMillis();
		if (now == lastID) {
			lastID = now + 1;
		} else {
			lastID = now;
		}
		return lastID;
	}
}
