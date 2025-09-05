package m15xb7.fix.service;

import m15xb7.fix.Order;
import m15xb7.fix.ParentOrder;
import m15xb7.fix.misc.ExecType;
import m15xb7.fix.misc.OrderListener;

public class UpstreamPublisher<R> implements OrderListener<R, ParentOrder<R>> {
	@Override
	public void onFill(ParentOrder<R> order, long qty, double px) {
		CharSequence use = order.view.getClOrdID();
	}

	@Override
	public void onNonFillExecutionReport(ParentOrder<R> order, ExecType execType) {
	}

	@Override
	public void onCancelReject(ParentOrder<R> order) {
	}

	@Override
	public void onReplaceReject(ParentOrder<R> order) {
	}
}
