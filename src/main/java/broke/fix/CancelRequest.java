package broke.fix;

import broke.fix.misc.CxlRejReason;
import broke.fix.misc.OrdStatus;
import broke.fix.misc.ExecType;
import broke.fix.misc.FixFields;
import broke.fix.Order;

public class CancelRequest<F extends FixFields> extends Request<F> {
	public CancelRequest(Order<F> order) {
		super(order);
	}

	@Override
	protected void init(CharSequence clOrdID) {
		if (order.view().isRoot() ^ (clOrdID == null)) {
			throw new RuntimeException("clOrdID should be passed to cancel request on root, and only on root");
		}
		order.begin();
		super.init(clOrdID != null ? clOrdID : order.idgen.getClOrdID());
		order.end(l->l.onCancelRequest(order));
	}
	
	@Override
	public void reject() {
		order.begin();
		super.reject();
		order.end(l->l.onCancelReject(order, CxlRejReason.TooLateToCancel));
	}

	@Override
	public OrdStatus getStatus() {
		return isPending() ? OrdStatus.PendingCancel : null;
	}
	
	@Override
	public CharSequence getOrigClOrdID() {
		if (order.replaceRequest.isPending() && !order.view().isRoot()) { //optimistic
			return order.replaceRequest.getClOrdID();
		}
		return getOrigClOrdID();
	}
}
