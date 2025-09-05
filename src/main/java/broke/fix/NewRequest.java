package broke.fix;

import broke.fix.misc.OrdStatus;
import broke.fix.misc.ExecType;
import broke.fix.Order;

public class NewRequest<F> extends Request<F> {
	private CharSequence downstreamOrderID;

	public NewRequest(Order<F> order) {
		super(order);
	}

	public void accept() {
		accept(null);
	}

	public void accept(CharSequence downstreamOrderID) {
		order.begin();
		super.accept();
		if (downstreamOrderID != null) {
			this.downstreamOrderID = downstreamOrderID;
		}
		order.end(l->l.onNonFillExecutionReport(order, ExecType.New));
	}


	@Override
	public void reject() {
		super.reject();
		order.terminate(OrdStatus.Rejected, ExecType.Rejected);
	}

	@Override
	public OrdStatus getStatus() {
		return isAccepted() ? OrdStatus.New : isPending() ? OrdStatus.PendingNew : OrdStatus.Rejected;
	}

	public CharSequence getDownstreamOrderID() {
		return downstreamOrderID;
	}
}
