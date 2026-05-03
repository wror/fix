package broke.fix.request;

import broke.fix.Order;
import broke.fix.Request;
import broke.fix.dto.ExecType;
import broke.fix.dto.OrdStatus;
import broke.fix.misc.FixFields;

public final class NewRequest<O extends Order<F>, F extends FixFields> extends Request<O, F> {
	public NewRequest(O order) {
		super(order.nextClOrdID(), order);
	}

	public NewRequest(CharSequence clOrdID, O order) {
		super(clOrdID, order);
	}

	@Override
	public OrdStatus getPendingStatus() {
		return OrdStatus.PendingNew;
	}
	
	@Override
	public void accept() {
		setStatus(Status.Accepted);
		getOrder().endTransaction(l->l.onOtherExecutionReport(getOrder(), ExecType.New, null, null));
	}

	@Override
	public void reject(Object reason) {
		setStatus(Status.Rejected);
		getOrder().terminate(OrdStatus.Rejected, ExecType.Rejected, reason);
	}

	@Override
	protected void onFill() {
		accept();
	}
}