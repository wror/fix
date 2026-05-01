package broke.fix.request;

import broke.fix.Order;
import broke.fix.Request;
import broke.fix.dto.CxlRejReason;
import broke.fix.dto.ExecType;
import broke.fix.dto.OrdStatus;
import broke.fix.misc.FixFields;

public final class CancelRequest<F extends FixFields> extends Request<F> {
	public CancelRequest(CharSequence origClOrdID, CharSequence clOrdID, Order<F> order) {
		super(origClOrdID, clOrdID, order);
	}

	@Override
	public void onAccept() {
		terminate(OrdStatus.Canceled, ExecType.Canceled, null); //TODO param for reason?
	}

	@Override
	public void onReject(Object reason) {
		endTransaction(l->l.onCancelReject(order, getClOrdID(), cxlRejReason(reason)));
	}

	@Override
	public OrdStatus getPendingStatus() {
		return OrdStatus.PendingCancel;
	}

	@Override
	protected void onFill() {
		reject(CxlRejReason.TooLateToCancel);
	}
}
