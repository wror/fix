package m15xb7.fix;

import m15xb7.fix.misc.OrdStatus;
import m15xb7.fix.misc.ExecType;
import m15xb7.fix.Request;
import m15xb7.fix.Order;

public class RequestReplace<F> extends Request<F> {
	private F pendingFields;
	private long pendingOrderQty;

	public RequestReplace(Order<F, ?> order) {
		super(order);
	}

	public void init(CharSequence clOrdID, F newFields, long newOrderQty) {
		super.init(clOrdID);
		this.pendingFields = newFields;
		this.pendingOrderQty = newOrderQty;
	}

	@Override
	public void reject() {
		order.begin();
		super.reject();
		order.end(l->l.onReplaceReject(order));
	}

	public long getQty() {
		return pendingOrderQty;
	}

	public F getFields() {
		return pendingFields;
	}

	@Override
	public void accept() {
		if (!isPending()) {
			return;
		}
		order.replace(pendingFields, pendingOrderQty);
		super.accept();
	}

	@Override
	public OrdStatus getStatus() {
		return isPending() ? OrdStatus.PendingReplace : null;
	}
}
