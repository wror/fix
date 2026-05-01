package broke.fix.test;

import broke.fix.CompositeOrder;
import broke.fix.Order;
import broke.fix.dto.CxlRejReason;
import broke.fix.dto.CxlRejResponseTo;
import broke.fix.dto.ExecRestatementReason;
import broke.fix.dto.ExecType;
import broke.fix.dto.OrdRejReason;
import broke.fix.misc.OrderListener;

import java.util.ArrayDeque;

class UpstreamPublisher implements OrderListener<CompositeOrder<Fields.Upstream>> {
	ArrayDeque<Message> queue = new ArrayDeque<>();
	
	@Override
	public void onNewRequest(CompositeOrder<Fields.Upstream> order) {
		queue.add(new Message(ExecType.PendingNew, order.getOrdStatus(), 0, order.getLeavesQty(), 0, "", ""));
	}
	
	@Override
	public void onTrade(CompositeOrder<Fields.Upstream> order, ExecType execType, long qty, double px) {
		Order v = order;
		queue.add(new Message(execType, v.getOrdStatus(), v.getCumQty(), v.getLeavesQty(), 0, "", null));
	}
	
	@Override
	public void onOtherExecutionReport(CompositeOrder<Fields.Upstream> order, ExecType execType, OrdRejReason reason, ExecRestatementReason execRestatementReason) {
		Order v = order;
		queue.add(new Message(execType, v.getOrdStatus(), v.getCumQty(), v.getLeavesQty(), 0, "", null));
	}

	@Override
	public void onCancelReject(CompositeOrder<Fields.Upstream> order, CharSequence clOrdID, CxlRejReason rejectReason) {
		Order v = order;
		queue.add(new Message(CxlRejResponseTo.Cancel, v.getOrdStatus(), clOrdID, ""));
	}
}
