package broke.fix.request;

import static broke.fix.misc.FixException.cxlRejReason;

import broke.fix.Order;
import broke.fix.Request;
import broke.fix.dto.OrdStatus;
import broke.fix.misc.FixException.Reason;
import broke.fix.misc.FixFields;

@SuppressWarnings("unchecked")
public final class ReplaceRequest<F extends FixFields> extends Request {
	private F requestedFields;

	public ReplaceRequest(Order<F> order, F requestedFields) {
		this(order, requestedFields, order.getClOrdID(), order.nextClOrdID());
	}

	public ReplaceRequest(Order<F> order, F requestedFields, CharSequence origClOrdID, CharSequence clOrdID) {
		super(order, origClOrdID, clOrdID);
		this.requestedFields = requestedFields;
		endTransaction(l->l.onReplaceRequest(order, this));
	}
	
	@Override
	public void accept() {
		setStatus(Status.Accepted);
		((Order<F>)order).replace(requestedFields);
	}

	@Override
	public void reject(Reason reason) {
		setStatus(Status.Rejected);
		endTransaction(l->l.onCancelOrReplaceReject(order, clOrdID, cxlRejReason(reason)));
	}

	public F getRequestedFields() {
		return requestedFields;
	}

	@Override
	public long getRequestedOrderQty() {
		return requestedFields.getOrderQty();
	}

	@Override
	protected OrdStatus getPendingOrdStatus() {
		return OrdStatus.PendingReplace;
	}

	@Override
	protected void onFill() {
		if (order.isFullyFilled() && getRequestedOrderQty() <= order.getFields().getOrderQty()) {
			reject(Reason.TooLate);
		}
		//could accept an amend-up that was sent before the order filled;
		// this is why Filled is not a terminal order status
	}
}
