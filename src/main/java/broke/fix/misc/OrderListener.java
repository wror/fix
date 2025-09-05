package broke.fix.misc;

import broke.fix.Member;
import broke.fix.Order;

public interface OrderListener<F, H extends Member<F, H>> {
	default void onNewRequest(H order) {}
	default void onCancelRequest(H order) {}
	default void onReplaceRequest(H order) {}
	default void onExecutionReport(H order, ExecType execType, long qty, double px) {}
	default void onCancelReject(H order, CxlRejReason rejectReason) {}
	default void onReplaceReject(H order, CxlRejReason rejectReason) {}
}
