package broke.fix.request;

import broke.fix.Order;
import broke.fix.Request;
import broke.fix.dto.CxlRejReason;
import broke.fix.dto.OrdStatus;
import broke.fix.misc.FixFields;

public final class ReplaceRequest<O extends Order<F>, F extends FixFields> extends Request<O, F> {
	private F pendingFields;

	public ReplaceRequest(O order, F newFields) {
		this(order.nextClOrdID(), order, newFields);
	}

	public ReplaceRequest(CharSequence clOrdID, O order, F newFields) {
		super(clOrdID, order);
		this.pendingFields = newFields;
		getOrder().endTransaction(l->l.onReplaceRequest((ReplaceRequest)this));
	}
	
	@Override
	public void accept() {
		setStatus(Status.Accepted);
		getOrder().replace(pendingFields);
	}

	@Override
	public void reject(Object reason) {
		setStatus(Status.Rejected);
		getOrder().endTransaction(l->l.onCancelOrReplaceReject(getOrder(), getClOrdID(), cxlRejReason(reason)));
	}

	@Override
	public long getQty() {
		return pendingFields.getOrderQty();
	}

	@Override
	public OrdStatus getPendingStatus() {
		return OrdStatus.PendingReplace;
	}

	public F getRequestedFields() {
		return pendingFields;
	}

	@Override
	protected void onFill() {
		if (getOrder().isFullyFilled() && getQty() <= getOrder().getFields().getOrderQty()) {
			reject(CxlRejReason.TooLateToCancel);
		}
		//could accept an amend-up that was sent before the order filled;
		// this is why Filled is not a terminal order status
	}
}
