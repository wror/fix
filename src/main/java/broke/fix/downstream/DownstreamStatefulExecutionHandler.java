package broke.fix.downstream;

import broke.fix.Order;
import broke.fix.dto.ExecType;
import broke.fix.dto.OrdRejReason;
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
	public void handleExecutionReport(CharSequence execRefID, //TODO @Override?
			ExecType execType, long transactTime, CharSequence downstreamOrderID, CharSequence clOrdID, long orderQty, long lastQty, double lastPx, OrdRejReason reason, CharSequence text) {
		simpleHandler.incoming.transactTime = transactTime;
		Order<?> order = simpleHandler.repo.getByEither(downstreamOrderID, clOrdID);
		if (order == null) {
			return;
		}
		switch (execType) {
			case TradeCorrect:
				execRepo.getExecution(execRefID).correct(lastQty, lastPx);
				break;
			case TradeCancel:
				execRepo.getExecution(execRefID).bust();
				break;
			default:
				simpleHandler.handleExecutionReport(execType, transactTime, downstreamOrderID, clOrdID, orderQty, lastQty, lastPx, reason, text);
		}
	}
}
