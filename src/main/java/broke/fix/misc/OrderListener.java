package broke.fix.misc;

import broke.fix.Order;
import broke.fix.dto.CxlRejReason;
import broke.fix.dto.ExecType;
import broke.fix.dto.OrdRejReason;
import broke.fix.request.CancelRequest;
import broke.fix.request.NewRequest;
import broke.fix.request.ReplaceRequest;

public interface OrderListener<O extends Order<F>, F extends FixFields> {
	default void onNewRequest(O order, NewRequest request) {} //the request could be null
	default void onCancelRequest(O order, CancelRequest request) {}
	default void onReplaceRequest(O order, ReplaceRequest<F> request) {}
	default void onTrade(O order, ExecType execType, long qty, double px) {}
	default void onOtherExecutionReport(O order, ExecType execType, OrdRejReason ordRejReason, CharSequence text) {}
	default void onCancelOrReplaceReject(O order, CharSequence clOrdID, CxlRejReason rejectReason) {}
}
