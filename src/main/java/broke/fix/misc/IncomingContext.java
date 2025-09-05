package broke.fix.misc;

public class IncomingContext {
	public long transactTime;
	public CharSequence text;

	public long getTime() {
		return System.currentTimeMillis();
	}
}
