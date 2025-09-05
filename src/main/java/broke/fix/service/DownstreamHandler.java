package broke.fix.service;

import broke.fix.Order;
import broke.fix.Request;
import broke.fix.ReplaceRequest;
import broke.fix.misc.CxlRejResponseTo;
import broke.fix.misc.ExecType;
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
	private final IdGenerator idgen;

	@Inject
	public DownstreamHandler(IdGenerator idgen, SimpleOrderRepo<F, Order<F>> repo) {
		this.idgen = idgen;
		this.repo = repo;
	}

/*
publishing and listeners:

would be nice to avoid passing things through the entity that don't need to be persisted
would also be nice to avoid the indirection of listeners for basic publishing

to do so, need to be able to understand "what happened" to the entity, e.g. closing an order to be terminated, or a request to be accepted or rejected
and which entities where affected - thought that might only be a thing for funkier order types

perhaps compromise and have "new" or also "replace request" messages published downstream should be explicit?
*/

	public void handleExecutionReport(ExecType execType, long transactTime, CharSequence orderID, CharSequence clOrdID, long lastQty, double lastPx) {
		idgen.incomingTransactTime = transactTime;
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
