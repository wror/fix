package broke.fix.misc;

import broke.fix.Order;
import broke.fix.dto.CxlRejReason;
import broke.fix.dto.ExecRestatementReason;
import broke.fix.dto.ExecType;
import broke.fix.dto.OrdRejReason;

public interface OrderListener<O extends Order<?>> {
	default void onNewRequest(O order) {}
	default void onCancelRequest(O order) {}
	default void onReplaceRequest(O order) {}
	default void onTrade(O order, ExecType execType, long qty, double px) {}
	default void onOtherExecutionReport(O order, ExecType execType, OrdRejReason ordRejReason, ExecRestatementReason execRestatementReason) {}
	default void onCancelReject(O order, CharSequence clOrdID, CxlRejReason rejectReason) {}
}
