package broke.fix.downstream;

import java.util.Map;
import java.util.WeakHashMap;

import broke.fix.SendableOrder;
import broke.fix.request.NewRequest;

public class DownstreamRepository {
	private final Map<CharSequence, NewRequest> requestByClOrdID = new WeakHashMap<>();
	private final Map<CharSequence, SendableOrder<?>> orderByOrderID = new WeakHashMap<>();

	public DownstreamRepository() {
	}

	public void addNew(NewRequest request) {
		requestByClOrdID.put(request.clOrdID, request);
	}

	public NewRequest getNew(CharSequence clOrdID) {
		return requestByClOrdID.get(clOrdID);
	}

	public void put(CharSequence downstreamOrderID, NewRequest request) {
		SendableOrder<?> order = (SendableOrder<?>)request.order;
		order.setDownstreamOrderID(downstreamOrderID);
		orderByOrderID.put(downstreamOrderID, order);
		requestByClOrdID.remove(request.clOrdID);
	}

	public SendableOrder<?> getByEither(CharSequence downstreamOrderID, CharSequence clOrdID) {
		SendableOrder<?> order = get(downstreamOrderID);
		if (order != null) {
			return order;
		}
		NewRequest request = getNew(clOrdID);
		put(downstreamOrderID, request);
		return (SendableOrder<?>)request.order;
	}

	private SendableOrder<?> get(CharSequence downstreamOrderID) {
		return orderByOrderID.get(downstreamOrderID);
	}
}
