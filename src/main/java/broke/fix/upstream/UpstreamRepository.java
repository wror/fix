package broke.fix.upstream;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

import broke.fix.CompositeOrder;
import broke.fix.RequestWithUpstreamClOrdID;
import broke.fix.dto.CxlRejReason;
import broke.fix.dto.OrdRejReason;
import broke.fix.request.NewRequest;
import broke.fix.upstream.ReasonExceptions.CxlRejException;
import broke.fix.upstream.ReasonExceptions.OrdRejException;

public class UpstreamRepository {
	private final Map<CharSequence, SoftReference<CompositeOrder<?>>> orderByClOrdID = new HashMap<>();
	private final Map<Long, CompositeOrder<?>> orderByOrderID = new HashMap<>();
	private final static SoftReference<CompositeOrder<?>> nullReference = new SoftReference<>(null);

	public UpstreamRepository() {
	}

	public void add(NewRequest<?> request) {
		if (orderByClOrdID.containsKey(request.getClOrdID())) {
			throw new OrdRejException(OrdRejReason.DuplicateClOrdID);
		}
		orderByClOrdID.put(request.getClOrdID(), new SoftReference<>((CompositeOrder<?>)request.getOrder()));
		orderByOrderID.put(request.getOrder().getInternalOrderID(), (CompositeOrder<?>)request.getOrder());
	}

	public void add(RequestWithUpstreamClOrdID request) {
		if (orderByClOrdID.containsKey(request.getClOrdID())) {
			throw new CxlRejException(CxlRejReason.DuplicateClOrdID);
		}
		orderByClOrdID.put(request.getClOrdID(), new SoftReference<>((CompositeOrder<?>)request.getOrder()));
	}

	public void addForDuplicateChecking(CharSequence clOrdID) {
		orderByClOrdID.put(clOrdID, nullReference);
	}

	public CompositeOrder<?> get(CharSequence origClOrdID) {
		return orderByClOrdID.getOrDefault(origClOrdID, nullReference).get();
	}
}
