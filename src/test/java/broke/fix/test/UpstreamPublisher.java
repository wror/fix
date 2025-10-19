package broke.fix.test;

import broke.fix.Order;
import broke.fix.OrderComposite;
import broke.fix.dto.CxlRejReason;
import broke.fix.dto.CxlRejResponseTo;
import broke.fix.dto.ExecType;
import broke.fix.misc.OrderListener;

import java.util.ArrayDeque;

class UpstreamPublisher implements OrderListener<Fields.Upstream, OrderComposite<Fields.Upstream>> {
	ArrayDeque<Message> queue = new ArrayDeque<>();
	
	@Override
	public void onExecutionReport(OrderComposite<Fields.Upstream> order, ExecType execType, long qty, double px) {
		Order.View v = order.view();
		queue.add(new Message(execType, v.getOrdStatus(), v.getCumQty(), v.getLeavesQty(), qty, v.getClOrdID(), null));
	}

	@Override
	public void onCancelReject(OrderComposite<Fields.Upstream> order, CxlRejReason rejectReason) {
		Order.View v = order.view();
		queue.add(new Message(CxlRejResponseTo.Cancel, v.getOrdStatus(), order.getCancelClOrdID(), v.getClOrdID()));
	}

	@Override
	public void onReplaceReject(OrderComposite<Fields.Upstream> order, CxlRejReason rejectReason) {
		Order.View v = order.view();
		queue.add(new Message(CxlRejResponseTo.Replace, v.getOrdStatus(), order.getReplaceClOrdID(), v.getClOrdID()));
	}
}
