package broke.fix.downstream;

import broke.fix.Order;
import broke.fix.dto.CxlRejReason;
import broke.fix.dto.ExecType;
import broke.fix.dto.OrdRejReason;
import broke.fix.dto.OrdStatus;
import broke.fix.dto.Side;
import broke.fix.misc.ExecutionRepository;

import javax.inject.Inject;

public class DownstreamStatefulExecutionHandler {
	private final DownstreamHandler simpleHandler;
	private final ExecutionRepository execRepo;

	@Inject
	public DownstreamStatefulExecutionHandler(DownstreamHandler simpleHandler, ExecutionRepository execRepo) {
		this.simpleHandler = simpleHandler;
		this.execRepo = execRepo;
	}

	@SuppressWarnings("null")
	public void handleExecutionReport(CharSequence execRefID, CharSequence execID, ExecType execType, long transactTime, CharSequence downstreamOrderID,
			CharSequence clOrdID, long orderQty, long lastQty, double lastPx, OrdRejReason ordRejReason, CharSequence text,
			CharSequence instrumentID, OrdStatus ordStatus, Side side, long leavesQty, long cumQty) {
		simpleHandler.incoming.transactTime = transactTime;
		Order<?> order = simpleHandler.repo.getByEither(downstreamOrderID, clOrdID);
		if (order == null) {
			return;
		}
		switch (execType) {
			case TradeCorrect:
				if (simpleHandler.notYetApplied(execID)) {
					execRepo.getExecution(execRefID).correct(lastQty, lastPx);
				}
				break;
			case TradeCancel:
				if (simpleHandler.notYetApplied(execID)) {
					execRepo.getExecution(execRefID).bust();
				}
				break;
			default:
				simpleHandler.handleExecutionReport(execID, execType, transactTime, downstreamOrderID, clOrdID, orderQty, lastQty, lastPx, ordRejReason, text, instrumentID, ordStatus, side, leavesQty, cumQty);
		}
	}

	public void handleOrderCancelReject(CxlRejReason cxlRejReason, CharSequence downstreamOrderID, CharSequence origClOrdID, CharSequence clOrdID, OrdStatus ordStatus) {
		simpleHandler.handleOrderCancelReject(cxlRejReason, downstreamOrderID, origClOrdID, clOrdID, ordStatus);
	}
}
