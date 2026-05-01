package broke.fix.request;

import broke.fix.Order;
import broke.fix.Request;
import broke.fix.dto.CxlRejReason;
import broke.fix.dto.OrdStatus;
import broke.fix.misc.FixFields;

public final class ReplaceRequest<F extends FixFields> extends Request<F> {
	private F pendingFields;

	public ReplaceRequest(CharSequence origClOrdID, CharSequence clOrdID, Order<F> order, F newFields) {
		super(origClOrdID, clOrdID, order);
		this.pendingFields = newFields;
	}
	
	@Override
	public void onAccept() {
		replace(pendingFields);
	}

	@Override
	public void onReject(Object reason) {
		endTransaction(l->l.onCancelReject(order, getClOrdID(), cxlRejReason(reason)));
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
		if (getQty() <= order.getFields().getOrderQty()) {
			reject(CxlRejReason.TooLateToCancel);
		}
		//could accept an amend-up that was sent before the order filled;
		// this is why Filled is not a terminal order status
	}
}
