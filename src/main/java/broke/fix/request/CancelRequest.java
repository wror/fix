package broke.fix.request;

import static broke.fix.misc.FixException.cxlRejReason;

import broke.fix.Order;
import broke.fix.Request;
import broke.fix.dto.ExecType;
import broke.fix.dto.OrdStatus;
import broke.fix.misc.FixException.Reason;

@SuppressWarnings("unchecked")
public final class CancelRequest extends Request {
	public CancelRequest(Order<?> order) {
		this(order, order.getClOrdID(), order.nextClOrdID());
	}

	public CancelRequest(Order<?> order, CharSequence origClOrdID, CharSequence clOrdID) {
		super(order, origClOrdID, clOrdID);
		endTransaction(l->l.onCancelRequest(order, this));
	}

	@Override
	public void accept() {
		setStatus(Status.Accepted);
		order.terminate(OrdStatus.Canceled, ExecType.Canceled, null);
	}

	@Override
	public void reject(Reason reason) {
		setStatus(Status.Rejected);
		endTransaction(l->l.onCancelOrReplaceReject(order, clOrdID, cxlRejReason(reason)));
	}

	@Override
	public long getRequestedOrderQty() {
		return 0;
	}

	@Override
	protected OrdStatus getPendingOrdStatus() {
		return OrdStatus.PendingCancel;
	}

	@Override
	protected void onFill() {
		reject(Reason.TooLate);
	}
}
