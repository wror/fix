package broke.fix.misc;

import broke.fix.Order;
import broke.fix.Parental;
import broke.fix.misc.ExecType;
import broke.fix.misc.OrderListener;

public class ParentalOrderListener<F> implements OrderListener<F, Order<F>> {
	private final Parental<F> parental;

	public ParentalOrderListener(Parental<F> parental) {
		this.parental = parental;
	}

	@Override
	public void onNewRequest(Order<F> order) {
		parental.listeners.forEach(l->l.onNewRequest(parental));
	}

	@Override
	public void onCancelRequest(Order<F> order) {
		parental.listeners.forEach(l->l.onCancelRequest(parental));
	}

	@Override
	public void onReplaceRequest(Order<F> order) {
		parental.listeners.forEach(l->l.onReplaceRequest(parental));
	}

	@Override
	public void onFill(Order<F> order, long qty, double px) {
		parental.listeners.forEach(l->l.onFill(parental, qty, px));
	}

	@Override
	public void onNonFillExecutionReport(Order<F> order, ExecType execType) {
		parental.listeners.forEach(l->l.onNonFillExecutionReport(parental, execType));
	}

	@Override
	public void onCancelReject(Order<F> order, CxlRejReason rejectReason) {
		parental.listeners.forEach(l->l.onCancelReject(parental, rejectReason));
	}

	@Override
	public void onReplaceReject(Order<F> order, CxlRejReason rejectReason) {
		parental.listeners.forEach(l->l.onReplaceReject(parental, rejectReason));
	}
}
