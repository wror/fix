package m15xb7.fix;

import m15xb7.fix.HierarchyOrder;
import m15xb7.fix.Order;
import m15xb7.fix.misc.ExecType;
import m15xb7.fix.misc.OrderListener;

public class OrderToParentOrderListener<F, R> implements OrderListener<F, Order<F, R>> { //TODO how?
	OrderListener<F, ParentOrder<F>> parentListener;
	ParentOrder<F> parentOrder;

	public OrderToParentOrderListener(ParentOrder<F> parentOrder, OrderListener<F, ParentOrder<F>> parentListener) {
		this.parentListener = parentListener;
		this.parentOrder = parentOrder;
	}

	@Override
	public void onNew(Order<F, R> order) {
		parentListener.onNew(parentOrder);
	}

	@Override
	public void onCancelRequest(Order<F, R> order) {
		parentListener.onCancelRequest(parentOrder);
	}

	@Override
	public void onReplaceRequest(Order<F, R> order) {
		parentListener.onReplaceRequest(parentOrder);
	}

	@Override
	public void onFill(Order<F, R> order, long qty, double px) {
		parentListener.onFill(parentOrder, qty, px);
	}

	@Override
	public void onNonFillExecutionReport(Order<F, R> order, ExecType execType) {
		parentListener.onNonFillExecutionReport(parentOrder, execType);
	}

	@Override
	public void onCancelReject(Order<F, R> order) {
		parentListener.onCancelReject(parentOrder);
	}

	@Override
	public void onReplaceReject(Order<F, R> order) {
		parentListener.onReplaceReject(parentOrder);
	}
}
