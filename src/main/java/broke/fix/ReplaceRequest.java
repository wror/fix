package broke.fix;

import broke.fix.misc.CxlRejReason;
import broke.fix.misc.OrdStatus;
import broke.fix.misc.ExecType;
import broke.fix.Order;

public class ReplaceRequest<F> extends Request<F> {
	private long pendingOrderQty;
	private F pendingFields;

	public ReplaceRequest(Order<F> order) {
		super(order);
	}

	public void init(long newOrderQty, F newFields) {
		init(null, 0, newOrderQty, newFields);
	}

	public void init(CharSequence clOrdID, long origOrdModTime, long newOrderQty, F newFields) {
		order.begin();
		if (isPending()) {
			order.end(l->l.onReplaceReject(order, CxlRejReason.AlreadyPendingCancelOrReplace));
			return;
		}
		if (origOrdModTime > 0 && origOrdModTime != order.view().getTransactTime()) {
			order.end(l->l.onReplaceReject(order, CxlRejReason.OrigOrdModTimeNotTransactTime));
			return;
		}
		super.init(clOrdID);
		this.pendingFields = newFields;
		this.pendingOrderQty = newOrderQty;
		order.end(l->l.onReplaceRequest(order));
	}

	@Override
	public void reject() {
		order.begin();
		super.reject();
		order.end(l->l.onReplaceReject(order, CxlRejReason.Other));
	}

	@Override
	public void accept() {
		if (!isPending()) {
			return;
		}
		long newOrderQty = Math.max(pendingOrderQty, order.view().getCumQty());
		order.replace(pendingFields, newOrderQty - order.view().getOrderQty());
		super.accept();
	}

	@Override
	public OrdStatus getStatus() {
		return isPending() ? OrdStatus.PendingReplace : null;
	}

	public long getQty() {
		return pendingOrderQty;
	}

	public F getFields() {
		return pendingFields;
	}
}
