package broke.fix.misc;

public class IncomingContext {
	public long transactTime;
	public CharSequence text;
	public CharSequence responseText;

	public long getTime() {
		return System.currentTimeMillis();
	}
}
