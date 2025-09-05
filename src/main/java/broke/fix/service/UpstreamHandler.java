package broke.fix.service;

import broke.fix.Order;
import broke.fix.Parental;
import broke.fix.misc.CxlRejReason;
import broke.fix.misc.CxlRejResponseTo;
import broke.fix.misc.ExecType;
import broke.fix.misc.ExecType;
import broke.fix.misc.IncomingContext;
import broke.fix.misc.IdGenerator;
import broke.fix.misc.OrderListener;
import broke.fix.misc.SimplePool;
import broke.fix.misc.SimpleOrderRepo;
import broke.fix.misc.Validator;

import javax.inject.Inject;

import java.util.Collection;
import java.util.Map;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UpstreamHandler<F> {
	private final static Logger log = LogManager.getLogger();
	private final IncomingContext incoming;
	private final IdGenerator idgen;
	private final OrderListener<F, Parental<F>> publisher;
	private final SimpleOrderRepo<F, Parental<F>> repo;
	private final SimplePool<Order<F>> pool;
	private final Collection<Validator<F>> validators;

	@Inject
	public UpstreamHandler(IncomingContext incoming, IdGenerator idgen, OrderListener<F, Parental<F>> publisher, SimpleOrderRepo<F, Parental<F>> repo, SimplePool<Order<F>> pool, Collection<Validator<F>> validators) {
		this.incoming = incoming;
		this.idgen = idgen;
		this.publisher = publisher;
		this.repo = repo;
		this.pool = pool;
		this.validators = validators;
	}

	public void handleNew(CharSequence clOrdID, long orderQty, F fields, long transactTime) {
		Order<F> order = pool.acquire().init(clOrdID, orderQty, fields);
		Parental<F> parental = repo.acquire().init(order);
		if (repo.isDuplicateClOrdID(clOrdID)) {
			reject(parental, order); //TODO OrdRejReason.DuplicateClOrdID
			return;
		}
		for (Validator<F> validator : validators) {
			String message = validator.getInvalidMessage(parental.view().getOrderQty(), fields, parental.view());
			if (message != null) { //TODO publish the message as Text on the executionreport
				repo.recordNonOrderClOrdID(clOrdID);
				reject(parental, order);
				return;
			}
		}
		incoming.transactTime = transactTime;
		order.newRequest.accept();
		repo.addOrder(parental);
		publisher.onNonFillExecutionReport(parental, ExecType.New);
	}

	private void reject(Parental<F> parental, Order<F> order) {
		publisher.onNonFillExecutionReport(parental, ExecType.Rejected);
		repo.release(parental);
		pool.release(order); //TODO ugh
	}

	public void handleReplace(long orderID, CharSequence clOrdID, long origOrdModTime, long orderQty, F fields) {
		if (isDupe(clOrdID)) {
			publisher.onReplaceReject(null, CxlRejReason.DuplicateClOrdID);
			return;
		}
		Parental<F> order = repo.getOrder(orderID, clOrdID);
		if (order == null) {
			publisher.onReplaceReject(null, CxlRejReason.UnknownOrder);
			return;
		}
		for (Validator<F> validator : validators) {
			String message = validator.getInvalidMessage(orderQty, fields, order.view());
			if (message != null) {
				publisher.onReplaceReject(order, CxlRejReason.Other);
				return;
			}
		}
		order.requestReplace(clOrdID, origOrdModTime, orderQty, fields);
	}

	public void handleCancel(long orderID, CharSequence clOrdID) {
		if (isDupe(clOrdID)) {
			publisher.onCancelReject(null, CxlRejReason.DuplicateClOrdID);
			return;
		}
		Parental<F> order = repo.getOrder(orderID, clOrdID);
		if (order == null) {
			publisher.onCancelReject(null, CxlRejReason.UnknownOrder);
			return;
		}
		order.requestCancel(clOrdID);
	}

	private boolean isDupe(CharSequence clOrdID) {
		boolean isDupe = repo.isDuplicateClOrdID(clOrdID);
		repo.recordNonOrderClOrdID(clOrdID);
		return isDupe;
	}
}
