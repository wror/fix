package broke.fix.downstream;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import broke.fix.dto.CxlRejReason;
import broke.fix.dto.ExecType;
import broke.fix.dto.OrdRejReason;
import broke.fix.dto.OrdStatus;
import broke.fix.dto.Side;
import broke.fix.misc.IncomingContext;
import broke.fix.request.NewRequest;

import static broke.fix.misc.FixException.reason;

import java.util.HashSet;
import java.util.Set;

public class DownstreamHandler {
	private final static Logger log = LogManager.getLogger();
	private final Set<CharSequence> execIDs = new HashSet<>();
	final IncomingContext incoming;
	final DownstreamRepository repo;

	@Inject
	public DownstreamHandler(IncomingContext incoming, DownstreamRepository repo) {
		this.incoming = incoming;
		this.repo = repo;
	}

	public void handleExecutionReport(CharSequence execID, ExecType execType, long transactTime, CharSequence downstreamOrderID,
			CharSequence clOrdID, long orderQty, long lastQty, double lastPx, OrdRejReason ordRejReason, CharSequence text,
			CharSequence instrumentID, OrdStatus ordStatus, Side side, long leavesQty, long cumQty) {
		incoming.transactTime = transactTime;
		require(downstreamOrderID, "orderID");
		require(execID, "execID");
		require(execType, "execType");
		require(instrumentID, "instrumentID");
		require(ordStatus, "ordStatus");
		require(side, "side");
		require(cumQty, "cumQty");
		require(leavesQty, "leavesQty");
		try {
			switch (execType) {
				case Rejected:
					repo.getNew(clOrdID).reject(reason(ordRejReason));
					break;
				case New:
					NewRequest request = repo.getNew(clOrdID);
					repo.put(downstreamOrderID, request);
					request.accept();
					break;
				case TradeCancel:
				case Trade:
					if (notYetApplied(execID)) {
						repo.getByEither(downstreamOrderID, clOrdID).fill(lastQty, lastPx);
					}
					break;
				case Replaced:
					repo.getByEither(downstreamOrderID, clOrdID).acceptReplace(clOrdID);
					break;
				case Canceled:
					repo.getByEither(downstreamOrderID, clOrdID).cancel(text);
					break;
				case DoneForDay:
					repo.getByEither(downstreamOrderID, clOrdID).done();
					break;
				default:
					log.warn("No support for {}, not applying to {}/{}", execType, downstreamOrderID, clOrdID);
			}
		} catch (RuntimeException e) {
			log.warn("Couldn't apply to {}/{}", downstreamOrderID, clOrdID, e);
		}
	}

	boolean notYetApplied(CharSequence execID) {
		if (execIDs.add(execID)) {
			return true;
		} else {
			log.warn("Already applied execID {}", execID);
			return false;
		}
	}

	public void handleOrderCancelReject(CxlRejReason cxlRejReason, CharSequence downstreamOrderID, CharSequence origClOrdID, CharSequence clOrdID, OrdStatus ordStatus) {
		require(downstreamOrderID, "orderID");
		require(clOrdID, "clOrdID");
		require(origClOrdID, "origClOrdID");
		require(ordStatus, "ordStatus");
		try {
			repo.getByEither(downstreamOrderID, clOrdID).rejectRequest(clOrdID, reason(cxlRejReason));
		} catch (RuntimeException e) {
			log.warn("Couldn't apply to {}/{}", downstreamOrderID, clOrdID, e);
		}
	}

	private void require(Object value, String name) {
		if (value == null) {
			log.warn("Missing required field {}", name);
		}
	}
}
