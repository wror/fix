package m15xb7.fix;

import m15xb7.fix.misc.OrdStatus;
import m15xb7.fix.misc.ExecType;
import m15xb7.fix.Request;
import m15xb7.fix.Order;

public class RequestNew<F> extends Request<F> {
	public RequestNew(Order<F, ?> order) {
		super(order);
	}

	@Override
	public void reject() {
		order.begin();
		super.reject();
		order.terminate(OrdStatus.Rejected);
		order.end(l->l.onNonFillExecutionReport(order, ExecType.Rejected));
	}

	public void accept(CharSequence orderID) {
		super.accept();
		if (orderID != null) {
			order.orderID = orderID;
		}
	}

	@Override
	protected OrdStatus getStatus() {
		return isAccepted() ? OrdStatus.New : isPending() ? OrdStatus.PendingNew : OrdStatus.Rejected;
	}
}
