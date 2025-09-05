package broke.fix.misc;

import broke.fix.OrderComponent;
import broke.fix.Order;
import broke.fix.dto.CxlRejReason;
import broke.fix.dto.ExecType;

public interface OrderListener<F, H extends OrderComponent<F, H>> {
	default void onNewRequest(H order) {}
	default void onCancelRequest(H order) {}
	default void onReplaceRequest(H order) {}
	default void onExecutionReport(H order, ExecType execType, long qty, double px) {}
	default void onCancelReject(H order, CxlRejReason rejectReason) {}
	default void onReplaceReject(H order, CxlRejReason rejectReason) {}
}
