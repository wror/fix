package broke.fix;

import broke.fix.misc.OrdStatus;
import broke.fix.misc.ExecType;
import broke.fix.misc.FixFields;
import broke.fix.Order;

public class NewRequest<F extends FixFields> extends Request<F> {
	private CharSequence downstreamOrderID;
	CharSequence origClOrdID;

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
		order.end(l->l.onExecutionReport(order, ExecType.New, 0, 0));
	}


	@Override
	public void reject() {
		order.begin();
		super.reject();
		order.terminate(OrdStatus.Rejected, ExecType.Rejected);
		//order.terminate() calls order.end(), so we don't do it again here
	}

	@Override
	public OrdStatus getStatus() {
		return isAccepted() ? OrdStatus.New : isPending() ? OrdStatus.PendingNew : OrdStatus.Rejected;
	}

	public CharSequence getDownstreamOrderID() {
		return downstreamOrderID;
	}

	public CharSequence getOrigClOrdID() {
		return origClOrdID;
	}
}
