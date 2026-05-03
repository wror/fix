package broke.fix.request;

import broke.fix.Order;
import broke.fix.Request;
import broke.fix.dto.CxlRejReason;
import broke.fix.dto.ExecType;
import broke.fix.dto.OrdStatus;
import broke.fix.misc.FixFields;

public final class CancelRequest<O extends Order<F>, F extends FixFields> extends Request<O, F> {
	public CancelRequest(O order) {
		this(order.nextClOrdID(), order);
	}

	public CancelRequest(CharSequence clOrdID, O order) {
		super(clOrdID, order);
		getOrder().endTransaction(l->l.onCancelRequest((CancelRequest)this));
	}

	@Override
	public void accept() {
		setStatus(Status.Accepted);
		getOrder().terminate(OrdStatus.Canceled, ExecType.Canceled, null); //TODO param for reason?
	}

	@Override
	public void reject(Object reason) {
		setStatus(Status.Rejected);
		getOrder().endTransaction(l->l.onCancelOrReplaceReject(getOrder(), getClOrdID(), cxlRejReason(reason)));
	}

	@Override
	public long getQty() {
		return 0;
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
