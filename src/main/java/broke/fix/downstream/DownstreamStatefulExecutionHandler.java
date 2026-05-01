package broke.fix.downstream;

import broke.fix.Order;
import broke.fix.dto.ExecType;
import broke.fix.dto.OrdRejReason;
import broke.fix.misc.ExecutionRepository;
import broke.fix.misc.FixFields;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DownstreamStatefulExecutionHandler<F extends FixFields> {
	private final static Logger log = LogManager.getLogger();
	private final DownstreamHandler<?> simpleHandler;
	private final ExecutionRepository execRepo;

	@Inject
	public DownstreamStatefulExecutionHandler(DownstreamHandler<F> simpleHandler, ExecutionRepository execRepo) {
		this.simpleHandler = simpleHandler;
		this.execRepo = execRepo;
	}

	public void handleExecutionReport(CharSequence execRefID, //TODO @Override?
			ExecType execType, long transactTime, CharSequence downstreamOrderID, CharSequence clOrdID, long orderQty, long lastQty, double lastPx, OrdRejReason reason) {
		simpleHandler.incoming.transactTime = transactTime;
		Order<?> order = simpleHandler.repo.get(downstreamOrderID);
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
				simpleHandler.handleExecutionReport(execType, transactTime, downstreamOrderID, clOrdID, orderQty, lastQty, lastPx, reason);
		}
	}
}
