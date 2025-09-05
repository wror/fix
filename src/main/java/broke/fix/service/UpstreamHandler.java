package broke.fix.service;

import broke.fix.Order;
import broke.fix.Parental;
import broke.fix.misc.CxlRejReason;
import broke.fix.misc.CxlRejResponseTo;
import broke.fix.misc.ExecType;
import broke.fix.misc.FixFields;
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
import java.util.Set;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UpstreamHandler<F extends FixFields> {
	private final static Logger log = LogManager.getLogger();
	private final IncomingContext incoming;
	private final IdGenerator idgen;
	private final OrderListener<F, Parental<F>> publisher;
	private final Set<CharSequence> clOrdIDs;
	private final SimpleOrderRepo<F, Parental<F>> repo;
	private final SimplePool<Order<F>> pool;
	private final Collection<Validator<F>> validators;

	@Inject
	public UpstreamHandler(IncomingContext incoming, IdGenerator idgen, OrderListener<F, Parental<F>> publisher, Set<CharSequence> clOrdIDs, SimpleOrderRepo<F, Parental<F>> repo, SimplePool<Order<F>> pool, Collection<Validator<F>> validators) {
		this.incoming = incoming;
		this.idgen = idgen;
		this.publisher = publisher;
		this.clOrdIDs = clOrdIDs;
		this.repo = repo;
		this.pool = pool;
		this.validators = validators;
	}

	public void handleNewRequest(CharSequence clOrdID, F fields, long transactTime) {
		incoming.transactTime = transactTime;
		if (isDupe(clOrdID)) {
			publisher.onExecutionReport(null, ExecType.Rejected, 0, 0); //TODO OrdRejReason.DuplicateClOrdID
			return;
		}
		Order<F> order = pool.acquire().init(clOrdID, fields);
		Parental<F> parental = repo.acquire().init(order, publisher);
		for (Validator<F> validator : validators) {
			CharSequence message = validator.getInvalidMessage(fields, parental.view());
			if (message != null) { //TODO publish the message as Text on the executionreport
				order.newRequest.reject();
				repo.release(parental);
				pool.release(order);
				return;
			}
		}
		order.newRequest.accept();
		repo.addOrder(parental);
	}

	public void handleReplaceRequest(long orderID, CharSequence origClOrdID, CharSequence clOrdID, F fields) {
		Consumer<CxlRejReason> rejector = reason->publisher.onReplaceReject(null, reason);
		Parental<F> order = getOrder(orderID, clOrdID, rejector);
		if (order == null) {
			return;
		}
		for (Validator<F> validator : validators) {
			CharSequence message = validator.getInvalidMessage(fields, order.view());
			if (message != null) {
				rejector.accept(CxlRejReason.Other);
				return;
			}
		}
		repo.updateClOrdID(origClOrdID, clOrdID, order); //optimistic
		order.requestReplace(clOrdID, fields);
	}

	public void handleCancelRequest(long orderID, CharSequence origClOrdID, CharSequence clOrdID) {
		Parental<F> order = getOrder(orderID, clOrdID, reason->publisher.onCancelReject(null, reason));
		if (order == null) {
			return;
		}
		order.requestCancel(clOrdID);
	}

	private Parental<F> getOrder(long orderID, CharSequence clOrdID, Consumer<CxlRejReason> rejector) {
		if (isDupe(clOrdID)) {
			rejector.accept(CxlRejReason.DuplicateClOrdID);
			return null;
		}
		Parental<F> order = repo.getOrder(orderID, clOrdID);
		if (order == null) {
			rejector.accept(CxlRejReason.UnknownOrder);
			return null;
		}
		return order;
	}

	private boolean isDupe(CharSequence clOrdID) {
		boolean isDupe = clOrdIDs.contains(clOrdID);
		clOrdIDs.add(clOrdID);
		return isDupe;
	}
}
