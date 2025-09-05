package broke.fix;

import broke.fix.misc.CxlRejReason;
import broke.fix.misc.OrdStatus;
import broke.fix.misc.ExecType;
import broke.fix.Order;

public class CancelRequest<F> extends Request<F> {
	public CancelRequest(Order<F> order) {
		super(order);
	}

	@Override
	public void reject() {
		order.begin();
		super.reject();
		order.end(l->l.onCancelReject(order, CxlRejReason.TooLateToCancel));
	}

	@Override
	protected void accept() {
		//callers should call order.cancel() instead
	}

	@Override
	public OrdStatus getStatus() {
		return isPending() ? OrdStatus.PendingCancel : null;
	}
}

