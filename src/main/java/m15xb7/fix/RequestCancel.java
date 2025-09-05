package m15xb7.fix;

import m15xb7.fix.misc.OrdStatus;
import m15xb7.fix.misc.ExecType;
import m15xb7.fix.Request;
import m15xb7.fix.Order;

public class RequestCancel<F> extends Request<F> {
	public RequestCancel(Order<F, ?> order) {
		super(order);
	}

//TODO share with RequestReplace?
	@Override
	public void reject() {
		order.begin();
		super.reject();
		order.end(l->l.onCancelReject((Order)order));
	}

	@Override
	void accept() {
		if (order.view.isWorking()) {
			order.cancel();
		}
		super.accept();
	}

	@Override
	protected OrdStatus getStatus() {
		return isPending() ? OrdStatus.PendingCancel : null;
	}
}

