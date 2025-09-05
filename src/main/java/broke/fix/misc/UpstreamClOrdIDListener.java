package broke.fix.misc;

import broke.fix.Order;
import broke.fix.OrderComposite;
import broke.fix.dto.CxlRejReason;

/*
 * It's not very important practically to allow upstream FIX systems to never specify orderID,
 * but 1) it's technically required by FIX, and 2) this is the overall simplest & clearest i came up with.
 */
public class UpstreamClOrdIDListener<F extends FixFields> implements OrderListener<F, Order<F>> {
	private final FixRepository repo;

	public UpstreamClOrdIDListener(FixRepository repo) {
		this.repo = repo;
	}

	@Override
	public void onReplaceRequest(Order<F> order) {
		repo.removeClOrdID(order.view().getClOrdID());
		repo.putClOrdID(order.replaceRequest.getClOrdID(), order);
	}

	@Override
	public void onReplaceReject(Order<F> order, CxlRejReason rejectReason) {
		repo.removeClOrdID(order.replaceRequest.getClOrdID());
		repo.putClOrdID(order.view().getClOrdID(), order);
	}
}
