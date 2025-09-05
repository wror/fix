package broke.fix.service;

import broke.fix.Order;
import broke.fix.Request;
import broke.fix.ReplaceRequest;
import broke.fix.misc.CxlRejResponseTo;
import broke.fix.misc.ExecType;
import broke.fix.misc.IncomingContext;
import broke.fix.misc.IdGenerator;
import broke.fix.misc.OrdStatus;
import broke.fix.misc.SimpleOrderRepo;

import javax.inject.Inject;

import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DownstreamHandler<F> {
	private final static Logger log = LogManager.getLogger();
	private final SimpleOrderRepo<F, Order<F>> repo;
	private final IncomingContext incoming;
	private final IdGenerator idgen;

	@Inject
	public DownstreamHandler(IncomingContext incoming, IdGenerator idgen, SimpleOrderRepo<F, Order<F>> repo) {
		this.incoming = incoming;
		this.idgen = idgen;
		this.repo = repo;
	}

	public void handleExecutionReport(ExecType execType, long transactTime, CharSequence orderID, CharSequence clOrdID, long lastQty, double lastPx) {
		incoming.transactTime = transactTime;
		Order<F> order = repo.getOrder(orderID, clOrdID);
		if (order == null) {
			return;
		}
		switch (execType) {
			case Rejected:
				order.newRequest.reject();
				break;
			case New:
				accept(order, orderID);
				break;
			case Trade:
				if (order.newRequest.isPending()) {
					accept(order, orderID);
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
				log.warn("No support for {}, not applying to {}/{}", execType, orderID, clOrdID);
		}
	}

	private void accept(Order<F> order, CharSequence orderID) {
		order.newRequest.accept(orderID);
		repo.addOrder(order);
	}

	public void handleOrderCancelReject(CxlRejResponseTo responseTo, CharSequence orderID, CharSequence clOrdID) {
		Order<F> order = repo.getOrder(orderID, clOrdID);
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
			log.warn("Unknown CxlRejResponseTo of {}, not applying to {}/{}", responseTo, orderID, clOrdID);
		}
	}

	private void logOfNonMatching(CharSequence incomingClOrdID, Request request) {
		if (!Objects.equals(incomingClOrdID, request.getClOrdID())) {
			log.warn("{} accept/reject with nonmatching clOrdID: {} vs {}", request.getClass().getSimpleName(), incomingClOrdID, request.getClOrdID());
		}
	}
}
