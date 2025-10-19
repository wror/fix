package broke.fix;

import broke.fix.dto.CxlRejReason;
import broke.fix.dto.OrdStatus;
import broke.fix.dto.ExecType;
import broke.fix.misc.FixFields;
import broke.fix.Order;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ReplaceRequest<F extends FixFields> extends Request<F> {
	private final static Logger log = LogManager.getLogger();
	private F pendingFields;

	public ReplaceRequest(Order<F> order) {
		super(order);
	}

	protected void init(CharSequence clOrdID, F newFields) {
		if (isPending()) {
			order.end(l->l.onReplaceReject(order, CxlRejReason.AlreadyPendingCancelOrReplace));
			return;
		}
		if (newFields.getOrigOrdModTime() > 0 && newFields.getOrigOrdModTime() != order.view().getTransactTime()) { //TODO check again before accepting?
			order.end(l->l.onReplaceReject(order, CxlRejReason.OrigOrdModTimeNotTransactTime));
			return;
		}

		//don't support outgoing "blind" replace request
		//since we only key repo by a single clordid per order, that would prevent supporting the more important blind cancel request
		if (!order.view().isRoot() && !order.newRequest.getClOrdID().isEmpty() && order.newRequest.getOrderID().isEmpty()) {
			order.end(l->l.onReplaceReject(order, CxlRejReason.Other));
			return;
		}
		super.init(clOrdID);
		this.pendingFields = newFields;
		order.end(l->l.onReplaceRequest(order));
	}

	@Override
	public void reject() {
		super.reject();
		order.end(l->l.onReplaceReject(order, CxlRejReason.Other));
	}

	@Override
	public void accept() {
		if (order.canReplace(pendingFields)) {
			super.accept();
		}
		order.replace(pendingFields); //calls order.end(), so don't call order.end() again here
	}

	@Override
	public OrdStatus getStatus() {
		return isPending() ? OrdStatus.PendingReplace : null;
	}

	public long getQty() {
		if (!isPending()) {
			return 0;
		}
		return pendingFields.getOrderQty();
	}

	public F getFields() {
		if (!isPending()) {
			return null;
		}
		return pendingFields;
	}
}
