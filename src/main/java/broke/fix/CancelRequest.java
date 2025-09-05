package broke.fix;

import broke.fix.dto.CxlRejReason;
import broke.fix.dto.OrdStatus;
import broke.fix.dto.ExecType;
import broke.fix.misc.FixFields;
import broke.fix.Order;

public class CancelRequest<F extends FixFields> extends Request<F> {
	public CancelRequest(Order<F> order) {
		super(order);
	}

	@Override
	protected void init(CharSequence clOrdID) {
		order.begin();
		super.init(clOrdID);
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
		return order.view().isRoot() ? order.view().getClOrdID() : order.view().getOptimisticClOrdID();
	}
}
