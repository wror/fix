package broke.fix.downstream;

import java.util.HashMap;
import java.util.Map;

import broke.fix.DownstreamOrder;
import broke.fix.RequestWithDownstreamOrderID;
import broke.fix.request.NewRequest;

public class DownstreamRepository {
	private final Map<CharSequence, NewRequest<?>> requestByClOrdID = new HashMap<>();
	private final Map<CharSequence, DownstreamOrder<?>> orderByOrderID = new HashMap<>();

	public DownstreamRepository() {
	}

	public void addNew(NewRequest<?> request) {
		requestByClOrdID.put(request.getClOrdID(), request);
	}

	public NewRequest<?> getNew(CharSequence clOrdID) {
		return requestByClOrdID.get(clOrdID);
	}

	public DownstreamOrder<?> get(CharSequence downstreamOrderID) {
		return (DownstreamOrder<?>)orderByOrderID.get(downstreamOrderID);
	}

	public RequestWithDownstreamOrderID add(RequestWithDownstreamOrderID request) {
		DownstreamOrder<?> order = (DownstreamOrder<?>)request.getOrder();
		orderByOrderID.put(request.getDownstreamOrderID(), order);
		requestByClOrdID.remove(request.getClOrdID());
		return request;
	}
}
