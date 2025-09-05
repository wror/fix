package m15xb7.fix.service;

import m15xb7.fix.Order;
import m15xb7.fix.misc.OrderListener;

public class DownstreamPublisher<F, R> implements OrderListener<F, Order<F, R>> {
	@Override
	public void onNew(Order<F, R> order) {
	}

	@Override
	public void onCancelRequest(Order<F, R> order) {
		CharSequence use = order.view.getClOrdID();
	}

	@Override
	public void onReplaceRequest(Order<F, R> order) {
	}

}
