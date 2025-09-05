package m15xb7.fix;

import m15xb7.fix.misc.OrderListener;
import m15xb7.fix.misc.ExecType;

public class WorkingQtyListener<F, R> implements OrderListener<F, Order<F, R>> {
	@Override
	public void onReplaceRequest(Order<F, R> order) {
		order.notifyParentOfWorkingQtyChange();
	}

	@Override
	public void onReplaceReject(Order<F, R> order) {
		order.notifyParentOfWorkingQtyChange();
	}

	@Override
	public void onFill(Order<F, R> order, long qty, double px) {
		order.notifyParentOfWorkingQtyChange();
	}

	@Override
	public void onNonFillExecutionReport(Order<F, R> order, ExecType execType) {
		order.notifyParentOfWorkingQtyChange();
	}
}
