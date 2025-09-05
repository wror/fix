package m15xb7.fix.misc;

import m15xb7.fix.HierarchyOrder;
import m15xb7.fix.Order;

public interface OrderListener<F, H extends HierarchyOrder<F, H>> {
	default void onNew(H order) {}
	default void onCancelRequest(H order) {}
	default void onReplaceRequest(H order) {}
	default void onFill(H order, long qty, double px) {}
	default void onNonFillExecutionReport(H order, ExecType execType) {}
	default void onCancelReject(H order) {}
	default void onReplaceReject(H order) {}
}
