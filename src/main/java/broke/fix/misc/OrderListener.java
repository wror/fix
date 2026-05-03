package broke.fix.misc;

import broke.fix.Order;
import broke.fix.dto.CxlRejReason;
import broke.fix.dto.ExecRestatementReason;
import broke.fix.dto.ExecType;
import broke.fix.dto.OrdRejReason;
import broke.fix.request.CancelRequest;
import broke.fix.request.ReplaceRequest;

public interface OrderListener<O extends Order<?>> {
	default void onNewRequest(O order) {}
	default void onCancelRequest(CancelRequest<O, ?> request) {}
	default void onReplaceRequest(ReplaceRequest<O, ?> request) {}
	default void onTrade(O order, ExecType execType, long qty, double px) {}
	default void onOtherExecutionReport(O order, ExecType execType, OrdRejReason ordRejReason, ExecRestatementReason execRestatementReason) {}
	default void onCancelOrReplaceReject(O order, CharSequence clOrdID, CxlRejReason rejectReason) {} //TODO alright to not have the request?
}
