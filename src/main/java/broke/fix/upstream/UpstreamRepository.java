package broke.fix.upstream;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

import broke.fix.CompositeOrder;
import broke.fix.Request;
import broke.fix.dto.CxlRejReason;
import broke.fix.misc.FixFields;
import broke.fix.upstream.ReasonExceptions.CxlRejException;

public class UpstreamRepository<F extends FixFields> {
	private final Map<CharSequence, SoftReference<CompositeOrder<F>>> orderByClOrdID = new HashMap<>();
	private final Map<Long, CompositeOrder<F>> orderByOrderID = new HashMap<>();
	private final static SoftReference nullReference = new SoftReference(null);

	public UpstreamRepository() {
	}

	public void add(Request<CompositeOrder<F>, F> request) {
		if (orderByClOrdID.containsKey(request.getClOrdID())) {
			//TODO obviously not the right exception for News
			throw new CxlRejException(CxlRejReason.DuplicateClOrdID);
		}
		orderByClOrdID.put(request.getClOrdID(), new SoftReference<>((CompositeOrder<F>)request.getOrder()));
	}

	public void add(CompositeOrder<F> order) {
		orderByOrderID.put(order.getInternalOrderID(), order);
	}

	public void remove(CompositeOrder<F> order) {
		orderByOrderID.remove(order.getInternalOrderID());
	}

	public void addJustForDuplicateChecking(CharSequence clOrdID) {
		orderByClOrdID.put(clOrdID, nullReference);
	}

	public CompositeOrder<F> get(CharSequence origClOrdID) {
		return orderByClOrdID.getOrDefault(origClOrdID, (SoftReference<CompositeOrder<F>>)nullReference).get();
	}
}
