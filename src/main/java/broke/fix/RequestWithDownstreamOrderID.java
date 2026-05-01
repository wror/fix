package broke.fix;

import broke.fix.request.NewRequest;

public class RequestWithDownstreamOrderID {
	private static RequestWithDownstreamOrderID instance = new RequestWithDownstreamOrderID();
	private DownstreamOrder<?> order;
	private NewRequest<?> request;

	//this class and its use improves slightly when Request is parameterized by the Order (e.g. DownstreamOrder) instead of its fields
	public static RequestWithDownstreamOrderID with(CharSequence orderID, NewRequest<?> request) {
		instance.request = request;
		instance.order = (DownstreamOrder<?>) request.getOrder();
		instance.order.downstreamOrderID = orderID;
		return instance;
	}

	public DownstreamOrder<?> getOrder() {
		return order;
	}

	public CharSequence getDownstreamOrderID() {
		return order.downstreamOrderID;
	}

	public CharSequence getClOrdID() {
		return request.getClOrdID();
	}

	public void accept() {
		request.accept();
	}
}