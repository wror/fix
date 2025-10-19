package broke.fix.service;

import broke.fix.Order;
import broke.fix.OrderComposite;
import broke.fix.dto.CxlRejReason;
import broke.fix.dto.CxlRejResponseTo;
import broke.fix.dto.ExecType;
import broke.fix.misc.FixFields;
import broke.fix.misc.IncomingContext;
import broke.fix.misc.IdGenerator;
import broke.fix.misc.OrderListener;
import broke.fix.misc.SimplePool;
import broke.fix.misc.FixRepository;
import broke.fix.misc.UpstreamClOrdIDListener;
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
	private final OrderListener<F, OrderComposite<F>> publisher;
	private final Set<CharSequence> clOrdIDs;
	private final FixRepository<F, OrderComposite<F>> repo;
	private final UpstreamClOrdIDListener upstreamClOrdIDListener;
	private final SimplePool<Order<F>> pool;
	private final Collection<Validator<F>> validators;

	@Inject
	public UpstreamHandler(IncomingContext incoming, IdGenerator idgen, OrderListener<F, OrderComposite<F>> publisher, Set<CharSequence> clOrdIDs, FixRepository<F, OrderComposite<F>> repo, SimplePool<Order<F>> pool, Collection<Validator<F>> validators) {
		this.incoming = incoming;
		this.idgen = idgen;
		this.publisher = publisher;
		this.clOrdIDs = clOrdIDs;
		this.repo = repo;
		this.upstreamClOrdIDListener = new UpstreamClOrdIDListener(repo);
		this.pool = pool;
		this.validators = validators;
	}

	public void handleNewRequest(CharSequence clOrdID, F fields, long transactTime) {
		incoming.transactTime = transactTime;
		if (isDupe(clOrdID)) {
			incoming.responseText = "duplicate ClOrdID";
			publisher.onExecutionReport(null, ExecType.Rejected, 0, 0);
			return;
		}
		Order<F> order = null;
		OrderComposite<F> composite = null;
		try {
			order = pool.acquire();
			order.init(clOrdID, fields);
			composite = repo.acquire();
			composite.init(order, publisher);
			for (Validator<F> validator : validators) {
				CharSequence message = validator.getInvalidMessage(fields, composite.view());
				if (message != null) {
					incoming.responseText = message;
					order.newRequest.reject();
					repo.release(composite);
					pool.release(order);
					return;
				}
			}
			order.listeners.add(upstreamClOrdIDListener);
			order.newRequest.accept();
			repo.addOrder(composite);
		} catch (RuntimeException e) {
			repo.release(composite);
			pool.release(order);
			incoming.responseText = e.getMessage();
			log.warn("Rejected because of exception: {}", incoming.responseText); //TODO why not seeing this in tests?
			publisher.onExecutionReport(null, ExecType.Rejected, 0, 0);
		}
	}

	public void handleReplaceRequest(CharSequence orderID, CharSequence origClOrdID, CharSequence clOrdID, F fields) {
		Consumer<CxlRejReason> rejector = reason->publisher.onReplaceReject(null, reason);
		OrderComposite<F> order = getOrder(orderID, clOrdID, rejector);
		if (order == null) {
			return;
		}
		for (Validator<F> validator : validators) {
			CharSequence message = validator.getInvalidMessage(fields, order.view());
			if (message != null) {
				incoming.responseText = message;
				rejector.accept(CxlRejReason.Other);
				return;
			}
		}
		order.requestReplace(clOrdID, fields);
	}

	public void handleCancelRequest(CharSequence orderID, CharSequence origClOrdID, CharSequence clOrdID) {
		OrderComposite<F> order = getOrder(orderID, clOrdID, reason->publisher.onCancelReject(null, reason));
		if (order == null) {
			return;
		}
		order.requestCancel(clOrdID);
	}

	private OrderComposite<F> getOrder(CharSequence orderID, CharSequence clOrdID, Consumer<CxlRejReason> rejector) {
		if (isDupe(clOrdID)) {
			rejector.accept(CxlRejReason.DuplicateClOrdID);
			return null;
		}
		OrderComposite<F> order = repo.getOrder(orderID, clOrdID);
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
