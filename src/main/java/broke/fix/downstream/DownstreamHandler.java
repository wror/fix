package broke.fix.downstream;

import static broke.fix.RequestWithDownstreamOrderID.with;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import broke.fix.DownstreamOrder;
import broke.fix.dto.CxlRejReason;
import broke.fix.dto.ExecType;
import broke.fix.dto.OrdRejReason;
import broke.fix.misc.FixFields;
import broke.fix.misc.IncomingContext;

public class DownstreamHandler<F extends FixFields> {
	private final static Logger log = LogManager.getLogger();
	final IncomingContext incoming;
	final DownstreamRepository repo;

	@Inject
	public DownstreamHandler(IncomingContext incoming, DownstreamRepository repo) {
		this.incoming = incoming;
		this.repo = repo;
	}

	public void handleExecutionReport(ExecType execType, long transactTime, CharSequence downstreamOrderID, CharSequence clOrdID, long orderQty, long lastQty, double lastPx, OrdRejReason reason) {
		incoming.transactTime = transactTime;
		switch (execType) {
			case Rejected:
				repo.getNew(clOrdID).reject(reason);
				break;
			case New:
				repo.add(with(downstreamOrderID, repo.getNew(clOrdID))).accept();
				break;
			case TradeCancel:
			case Trade:
				DownstreamOrder<?> order = repo.get(downstreamOrderID);
				if (order == null) {
					order = repo.add(with(downstreamOrderID, repo.getNew(clOrdID))).getOrder();
				}
				order.fill(lastQty, lastPx);
				break;
			case Replaced:
				repo.get(downstreamOrderID).acceptReplace(clOrdID, orderQty);
				break;
			case Canceled:
				repo.get(downstreamOrderID).cancel();
				break;
			case DoneForDay:
				repo.get(downstreamOrderID).done();
				break;
			default:
				log.warn("No support for {}, not applying to {}/{}", execType, downstreamOrderID, clOrdID);
		}
	}

	public void handleOrderCancelReject(CxlRejReason reason, CharSequence downstreamOrderID, CharSequence clOrdID) {
		repo.get(downstreamOrderID).rejectRequest(clOrdID, reason);
	}
}
