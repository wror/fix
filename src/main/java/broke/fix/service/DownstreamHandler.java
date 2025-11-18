package broke.fix.service;

import java.util.Objects;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import broke.fix.Order;
import broke.fix.Request;
import broke.fix.dto.CxlRejResponseTo;
import broke.fix.dto.ExecType;
import broke.fix.misc.FixFields;
import broke.fix.misc.FixRepository;
import broke.fix.misc.IncomingContext;

public class DownstreamHandler<F extends FixFields> {
	private final static Logger log = LogManager.getLogger();
	final IncomingContext incoming;
	final FixRepository<F, Order<F>> repo;

	@Inject
	public DownstreamHandler(IncomingContext incoming, FixRepository<F, Order<F>> repo) {
		this.incoming = incoming;
		this.repo = repo;
	}

	public void handleExecutionReport(ExecType execType, long transactTime, CharSequence downstreamOrderID,
			CharSequence clOrdID, CharSequence origClOrdID,
			long lastQty, double lastPx) {
		incoming.transactTime = transactTime;
		Order<F> order = repo.getOrder(downstreamOrderID, clOrdID);
		if (order == null) {
			return;
		}
		switch (execType) {
			case Rejected:
				order.newRequest.reject();
				break;
			case New:
				order.newRequest.accept(downstreamOrderID);
				repo.removeClOrdID(order.view().getClOrdID()); //now we don't need it in the repo, and we may not have a reference to remove it later, after replaces
				break;
			case TradeCancel:
			case Trade:
				if (order.newRequest.isPending()) {
					order.newRequest.accept(downstreamOrderID);
					repo.removeClOrdID(order.view().getClOrdID());
				}
				order.fill(lastQty, lastPx);
				break;
			case Replaced:
				logOfNonMatching(clOrdID, order.replaceRequest);
				order.replaceRequest.accept();
				break;
			case Canceled:
				order.cancel();
				break;
			case DoneForDay:
				order.done();
				break;
			default:
				log.warn("No support for {}, not applying to {}/{}", execType, downstreamOrderID, clOrdID);
		}
	}

	public void handleOrderCancelReject(CxlRejResponseTo responseTo, CharSequence downstreamOrderID, CharSequence clOrdID) {
		Order<F> order = repo.getOrder(downstreamOrderID, clOrdID);
		if (order == null) {
			return;
		}
		if (responseTo == CxlRejResponseTo.Cancel || Objects.equals(clOrdID, order.cancelRequest.getClOrdID())) {
			logOfNonMatching(clOrdID, order.cancelRequest);
			order.cancelRequest.reject();
		} else if (responseTo == CxlRejResponseTo.Replace || Objects.equals(clOrdID, order.replaceRequest.getClOrdID())) {
			logOfNonMatching(clOrdID, order.replaceRequest);
			order.replaceRequest.reject();
		} else {
			log.warn("Unknown CxlRejResponseTo of {}, not applying to {}/{}", responseTo, downstreamOrderID, clOrdID);
		}
	}

	private void logOfNonMatching(CharSequence incomingClOrdID, Request request) {
		if (!Objects.equals(incomingClOrdID, request.getClOrdID())) {
			log.warn("{} accept/reject with nonmatching clOrdID: {} vs {}", request.getClass().getSimpleName(), incomingClOrdID, request.getClOrdID());
		}
	}
}
