package broke.fix.request;

import broke.fix.Order;
import broke.fix.Request;
import broke.fix.dto.ExecType;
import broke.fix.dto.OrdStatus;
import broke.fix.misc.FixException.Reason;

@SuppressWarnings("unchecked")
public final class NewRequest extends Request {
	public NewRequest(Order<?> order) {
		this(order, order.nextClOrdID());
	}

	public NewRequest(Order<?> order, CharSequence clOrdID) {
		super(order, null, clOrdID);
		endTransaction(l->l.onNewRequest(order, this));
	}
	
	@Override
	public void accept() {
		setStatus(Status.Accepted);
		endTransaction(l->l.onOtherExecutionReport(order, ExecType.New, null, null));
	}

	@Override
	public void reject(Reason reason) {
		setStatus(Status.Rejected);
		order.terminate(OrdStatus.Rejected, ExecType.Rejected, reason.toString());
	}

	@Override
	protected long getRequestedOrderQty() {
		return order.getFields().getOrderQty();
	}

	@Override
	protected OrdStatus getPendingOrdStatus() {
		return OrdStatus.PendingNew;
	}

	@Override
	protected void onFill() {
		accept();
	}
}
