package broke.fix;

import broke.fix.misc.CxlRejReason;
import broke.fix.misc.OrderListener;
import broke.fix.misc.ExecType;

public class WorkingQtyListener<F> implements OrderListener<F, Order<F>> {
	@Override
	public void onReplaceRequest(Order<F> order) {
		order.notifyParentOfWorkingQtyChange();
	}

	@Override
	public void onReplaceReject(Order<F> order, CxlRejReason rejectReason) {
		order.notifyParentOfWorkingQtyChange();
	}

	@Override
	public void onFill(Order<F> order, long qty, double px) {
		order.notifyParentOfWorkingQtyChange();
	}

	@Override
	public void onNonFillExecutionReport(Order<F> order, ExecType execType) {
		order.notifyParentOfWorkingQtyChange();
	}
}
