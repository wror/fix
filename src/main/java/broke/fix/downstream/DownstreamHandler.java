package broke.fix.downstream;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import broke.fix.dto.CxlRejReason;
import broke.fix.dto.ExecType;
import broke.fix.dto.OrdRejReason;
import broke.fix.misc.IncomingContext;
import broke.fix.request.NewRequest;

import static broke.fix.misc.FixException.reason;

public class DownstreamHandler {
	private final static Logger log = LogManager.getLogger();
	final IncomingContext incoming;
	final DownstreamRepository repo;

	@Inject
	public DownstreamHandler(IncomingContext incoming, DownstreamRepository repo) {
		this.incoming = incoming;
		this.repo = repo;
	}

	public void handleExecutionReport(ExecType execType, long transactTime, CharSequence downstreamOrderID, CharSequence clOrdID, long orderQty, long lastQty, double lastPx, OrdRejReason ordRejReason, CharSequence text) {
		incoming.transactTime = transactTime;
		//TODO required orderID, execID, execType, ordStatus, instrument fields, side, cumQty
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
					repo.getByEither(downstreamOrderID, clOrdID).fill(lastQty, lastPx);
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

	//TODO required CxlRejResponseTo, ordStatus, clOrdID, origClOrdID, OrderID
	public void handleOrderCancelReject(CxlRejReason cxlRejReason, CharSequence downstreamOrderID, CharSequence clOrdID) {
		try {
			repo.getByEither(downstreamOrderID, clOrdID).rejectRequest(clOrdID, reason(cxlRejReason));
		} catch (RuntimeException e) {
			log.warn("Couldn't apply to {}/{}", downstreamOrderID, clOrdID, e);
		}
	}
}
