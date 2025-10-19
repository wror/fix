package broke.fix.service;

import broke.fix.Order;
import broke.fix.dto.ExecType;
import broke.fix.misc.Execution;
import broke.fix.misc.ExecutionRepository;
import broke.fix.misc.FixFields;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DownstreamStatefulExecutionHandler<F extends FixFields> {
	private final static Logger log = LogManager.getLogger();
	private final DownstreamHandler simpleHandler;
	private final ExecutionRepository execRepo;

	@Inject
	public DownstreamStatefulExecutionHandler(DownstreamHandler<F> simpleHandler, ExecutionRepository execRepo) {
		this.simpleHandler = simpleHandler;
		this.execRepo = execRepo;
	}

	public void handleExecutionReport(ExecType execType, long transactTime, CharSequence downstreamOrderID,
			CharSequence clOrdID, CharSequence origClOrdID,
			CharSequence execID, CharSequence execRefID,
			long lastQty, double lastPx) {
		simpleHandler.incoming.transactTime = transactTime;
		Order<F> order = (Order<F>)simpleHandler.repo.getOrder(downstreamOrderID, null); //TODO why cast nesc?
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
				simpleHandler.handleExecutionReport(execType, transactTime, downstreamOrderID, clOrdID, origClOrdID, lastQty, lastPx);
		}
	}
}
