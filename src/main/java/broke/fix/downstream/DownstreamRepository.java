package broke.fix.downstream;

import java.util.Map;
import java.util.WeakHashMap;

import broke.fix.SendableOrder;
import broke.fix.misc.FixFields;
import broke.fix.request.NewRequest;

public class DownstreamRepository<F extends FixFields> {
	private final Map<CharSequence, NewRequest<SendableOrder<F>, F>> requestByClOrdID = new WeakHashMap<>();
	private final Map<CharSequence, SendableOrder<F>> orderByOrderID = new WeakHashMap<>();

	public DownstreamRepository() {
	}

	public void addNew(NewRequest<SendableOrder<F>, F> request) {
		requestByClOrdID.put(request.getClOrdID(), request);
	}

	public NewRequest<SendableOrder<F>, F> getNew(CharSequence clOrdID) {
		return requestByClOrdID.get(clOrdID);
	}

	public SendableOrder<F> get(CharSequence downstreamOrderID) {
		return orderByOrderID.get(downstreamOrderID);
	}

	public void put(CharSequence downstreamOrderID, NewRequest<SendableOrder<F>, F> request) {
		SendableOrder<F> order = request.getOrder();
		order.setDownstreamOrderID(downstreamOrderID);
		orderByOrderID.put(downstreamOrderID, order);
		requestByClOrdID.remove(request.getClOrdID());
	}

	public SendableOrder<F> getHowever(CharSequence downstreamOrderID, CharSequence clOrdID) {
		SendableOrder<F> order = get(downstreamOrderID);
		if (order != null) {
			return order;
		}
		NewRequest<SendableOrder<F>, F> request = getNew(clOrdID);
		put(downstreamOrderID, request);
		return request.getOrder();
	}
}
