package broke.fix.request;

import broke.fix.Order;
import broke.fix.Request;
import broke.fix.dto.ExecType;
import broke.fix.dto.OrdStatus;
import broke.fix.misc.FixFields;

public final class NewRequest<F extends FixFields> extends Request<F> {
	public NewRequest(CharSequence clOrdID, Order<F> order) {
		super(null, clOrdID, order);
	}

	@Override
	public OrdStatus getPendingStatus() {
		return OrdStatus.PendingNew;
	}
	
	@Override
	public void onAccept() {
		endTransaction(l->l.onOtherExecutionReport(order, ExecType.New, null, null));
	}

	@Override
	public void onReject(Object reason) {
		terminate(OrdStatus.Rejected, ExecType.Rejected, reason);
	}

	@Override
	protected void onFill() {
		accept();
	}
}
