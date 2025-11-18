package broke.fix;

import broke.fix.dto.ExecType;
import broke.fix.dto.OrdStatus;
import broke.fix.misc.FixFields;

public class NewRequest<F extends FixFields> extends Request<F> {
	private StringBuffer orderID = new StringBuffer(16);

	public NewRequest(Order<F> order) {
		super(order);
	}

	public void accept() {
		accept(null);
	}

	@Override
	protected void init(CharSequence clOrdID) {
		super.init(clOrdID);
		this.orderID.setLength(0);
		//order.init() calls order.end() as well calling here, so we don't call order.end() again here
	}

	public void accept(CharSequence orderID) {
		order.begin();
		super.accept();
		if (orderID != null) {
			this.orderID.append(orderID);
		}
		order.end(l->l.onExecutionReport(order, ExecType.New, 0, 0));
	}

	@Override
	public void reject() {
		super.reject();
		order.terminate(OrdStatus.Rejected, ExecType.Rejected); //calls order.end(), so we don't call order.end() again here
	}

	@Override
	public OrdStatus getStatus() {
		return isAccepted() ? OrdStatus.New : isPending() ? OrdStatus.PendingNew : OrdStatus.Rejected;
	}

	public CharSequence getOrderID() {
		return orderID;
	}
}
