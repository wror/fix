package broke.fix.test;

import broke.fix.CompositeOrder;
import broke.fix.dto.CxlRejReason;
import broke.fix.dto.CxlRejResponseTo;
import broke.fix.dto.ExecType;
import broke.fix.dto.OrdRejReason;
import broke.fix.misc.OrderListener;
import broke.fix.request.NewRequest;

import java.util.ArrayDeque;
import javax.annotation.Nullable;

class UpstreamPublisher implements OrderListener<CompositeOrder<Fields.Upstream>, Fields.Upstream> {
	ArrayDeque<Message> queue = new ArrayDeque<>();
	
	@Override
	public void onNewRequest(CompositeOrder<Fields.Upstream> order, @Nullable NewRequest request) {
		queue.add(new Message(ExecType.PendingNew, order.getOrdStatus(), 0, order.getLeavesQty(), 0, "", ""));
	}
	
	@Override
	public void onTrade(CompositeOrder<Fields.Upstream> order, ExecType execType, long qty, double px) {
		queue.add(new Message(execType, order.getOrdStatus(), order.getCumQty(), order.getLeavesQty(), 0, "", null));
	}
	
	@Override
	public void onOtherExecutionReport(CompositeOrder<Fields.Upstream> order, ExecType execType, @Nullable OrdRejReason reason, @Nullable CharSequence text) {
		queue.add(new Message(execType, order.getOrdStatus(), order.getCumQty(), order.getLeavesQty(), 0, "", null));
	}

	@Override
	public void onCancelOrReplaceReject(CompositeOrder<Fields.Upstream> order, CharSequence clOrdID, CxlRejResponseTo responseto, @Nullable CxlRejReason rejectReason) {
		queue.add(new Message(CxlRejResponseTo.Cancel, order.getOrdStatus(), clOrdID, ""));
	}
}
