package broke.fix.downstream;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import broke.fix.SendableOrder;
import broke.fix.dto.CxlRejReason;
import broke.fix.dto.ExecType;
import broke.fix.dto.OrdRejReason;
import broke.fix.misc.FixFields;
import broke.fix.misc.IncomingContext;
import broke.fix.request.NewRequest;

public class DownstreamHandler<F extends FixFields> {
	private final static Logger log = LogManager.getLogger();
	final IncomingContext incoming;
	final DownstreamRepository<F> repo;

	@Inject
	public DownstreamHandler(IncomingContext incoming, DownstreamRepository<F> repo) {
		this.incoming = incoming;
		this.repo = repo;
	}

	public void handleExecutionReport(ExecType execType, long transactTime, CharSequence downstreamOrderID, CharSequence clOrdID, long orderQty, long lastQty, double lastPx, OrdRejReason reason) {
		incoming.transactTime = transactTime;
		//TODO required orderID, execID, execType, ordStatus, instrument fields, side, cumQty
		switch (execType) {
			case Rejected:
				repo.getNew(clOrdID).reject(reason);
				break;
			case New:
				NewRequest<SendableOrder<F>, F> request = repo.getNew(clOrdID);
				repo.put(downstreamOrderID, request);
				request.accept();
				break;
			case TradeCancel:
			case Trade:
				repo.getHowever(downstreamOrderID, clOrdID).fill(lastQty, lastPx);
				break;
			case Replaced:
				repo.getHowever(downstreamOrderID, clOrdID).acceptReplace(clOrdID);
				break;
			case Canceled:
				repo.getHowever(downstreamOrderID, clOrdID).cancel();
				break;
			case DoneForDay:
				repo.getHowever(downstreamOrderID, clOrdID).done();
				break;
			default:
				log.warn("No support for {}, not applying to {}/{}", execType, downstreamOrderID, clOrdID);
		}
	}

	//TODO required CxlRejResponseTo, ordStatus, clOrdID, origClOrdID, OrderID
	public void handleOrderCancelReject(CxlRejReason reason, CharSequence downstreamOrderID, CharSequence clOrdID) {
		repo.get(downstreamOrderID).rejectRequest(clOrdID, reason);
	}
}
