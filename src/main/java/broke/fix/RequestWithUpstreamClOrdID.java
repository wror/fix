package broke.fix;

public class RequestWithUpstreamClOrdID {
	private static RequestWithUpstreamClOrdID instance = new RequestWithUpstreamClOrdID();
	private Request<?> request;

	public static RequestWithUpstreamClOrdID with(CharSequence clOrdID, Request<?> request) {
		request.clOrdID = clOrdID;
		instance.request = request;
		return instance;
	}

	public Order<?> getOrder() {
		return request.getOrder();
	}

	public CharSequence getClOrdID() {
		return request.getClOrdID();
	}
}