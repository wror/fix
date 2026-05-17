package broke.fix.misc;

import javax.annotation.Nullable;

import broke.fix.Order;
import broke.fix.dto.CxlRejReason;
import broke.fix.dto.CxlRejResponseTo;
import broke.fix.dto.ExecType;
import broke.fix.dto.OrdRejReason;
import broke.fix.request.CancelRequest;
import broke.fix.request.NewRequest;
import broke.fix.request.ReplaceRequest;

public interface OrderListener<O extends Order<F>, F extends FixFields> {
	default void onNewRequest(O order, @Nullable NewRequest request) {}
	default void onCancelRequest(O order, CancelRequest request) {}
	default void onReplaceRequest(O order, ReplaceRequest<F> request) {}
	default void onTrade(O order, ExecType execType, long qty, double px) {}
	default void onOtherExecutionReport(O order, ExecType execType, @Nullable OrdRejReason ordRejReason, @Nullable CharSequence text) {}
	default void onCancelOrReplaceReject(O order, CharSequence clOrdID, CxlRejResponseTo responseTo, @Nullable CxlRejReason rejectReason) {}
}
