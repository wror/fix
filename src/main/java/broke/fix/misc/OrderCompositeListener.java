package broke.fix.misc;

import broke.fix.Order;
import broke.fix.OrderComposite;
import broke.fix.dto.CxlRejReason;
import broke.fix.dto.ExecType;
import broke.fix.dto.ExecType;
import broke.fix.misc.OrderListener;

public class OrderCompositeListener<F extends FixFields> implements OrderListener<F, Order<F>> {
	private final OrderComposite<F> composite;

	public OrderCompositeListener(OrderComposite<F> composite) {
		this.composite = composite;
	}

	@Override
	public void onNewRequest(Order<F> order) {
		composite.listeners().forEach(l->l.onNewRequest(composite));
	}

	@Override
	public void onCancelRequest(Order<F> order) {
		composite.listeners().forEach(l->l.onCancelRequest(composite));
	}

	@Override
	public void onReplaceRequest(Order<F> order) {
		composite.listeners().forEach(l->l.onReplaceRequest(composite));
	}

	@Override
	public void onExecutionReport(Order<F> order, ExecType execType, long qty, double px) {
		composite.listeners().forEach(l->l.onExecutionReport(composite, execType, qty, px));
	}

	@Override
	public void onCancelReject(Order<F> order, CxlRejReason rejectReason) {
		composite.listeners().forEach(l->l.onCancelReject(composite, rejectReason));
	}

	@Override
	public void onReplaceReject(Order<F> order, CxlRejReason rejectReason) {
		composite.listeners().forEach(l->l.onReplaceReject(composite, rejectReason));
	}
}
