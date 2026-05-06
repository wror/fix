package broke.fix.upstream;

import java.util.HashMap;
import java.util.Map;

import broke.fix.CompositeOrder;
import broke.fix.Request;
import broke.fix.misc.FixException;
import broke.fix.misc.FixFields;
import broke.fix.request.NewRequest;

public class UpstreamRepository<F extends FixFields> {
	private final Map<CharSequence, CompositeOrder<F>> orderByClOrdID = new HashMap<>();
	private final Map<Long, CompositeOrder<F>> orderByOrderID = new HashMap<>();

	public UpstreamRepository() {
	}

	public void checkForDuplicates(CharSequence clOrdID) throws FixException {
		if (orderByClOrdID.containsKey(clOrdID)) {
			throw new FixException(FixException.Reason.DuplicateClOrdID);
		}
	}

	@SuppressWarnings("unchecked")
	public void add(NewRequest request) {
		orderByClOrdID.put(request.clOrdID, (CompositeOrder<F>)request.order);
		add((CompositeOrder<F>)request.order);
	}

	private void add(CompositeOrder<F> order) {
		orderByOrderID.put(order.getInternalOrderID(), order);
	}

	public void addJustForDuplicateChecking(CharSequence clOrdID) {
		orderByClOrdID.put(clOrdID, null);
	}

	public CompositeOrder<F> get(CharSequence origClOrdID) {
		return orderByClOrdID.get(origClOrdID);
	}

	public CompositeOrder<F> get(long internalOrderID) {
		return orderByOrderID.get(internalOrderID);
	}

	public void remove(Request request) {
		addJustForDuplicateChecking(request.clOrdID);
	}

	public void remove(CompositeOrder<F> order) {
		orderByOrderID.remove(order.getInternalOrderID());
	}
}