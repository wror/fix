package broke.fix.service;

import broke.fix.Order;
import broke.fix.OrderComposite;
import broke.fix.dto.CxlRejReason;
import broke.fix.misc.FixFields;
import broke.fix.misc.IncomingContext;
import broke.fix.misc.IdGenerator;
import broke.fix.misc.OrderListener;
import broke.fix.misc.SimplePool;
import broke.fix.misc.FixRepository;
import broke.fix.misc.Validator;

import javax.inject.Inject;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;

public class UpstreamWithPassthroughHandler<F extends FixFields> extends UpstreamHandler<F> {
	private final PassthroughService passThroughService;
	private final Consumer<CxlRejReason> rejector;

	@Inject
	public UpstreamWithPassthroughHandler(PassthroughService passThroughService, IncomingContext incoming, IdGenerator idgen, OrderListener<F, OrderComposite<F>> publisher, Set<CharSequence> clOrdIDs, FixRepository<F, OrderComposite<F>> repo, SimplePool<Order<F>> pool, Collection<Validator<F>> validators) {
		super(incoming, idgen, publisher, clOrdIDs, repo, pool, validators);
		this.passThroughService = passThroughService;
		this.rejector = reason->publisher.onReplaceReject(null, reason);
	}
	
	@Override
	protected void handleNewOrder(Order<F> order, OrderComposite<F> composite) {
		super.handleNewOrder(order, composite);
		if (order.view().getFields().getExDestination() != null) {
			passThroughService.slicePassthrough(order, composite, order.view().getFields());
		}
	}

	@Override
	public void handleReplaceRequest(CharSequence orderID, CharSequence origClOrdID, CharSequence clOrdID, F fields) {
		OrderComposite<F> order = getOrder(orderID, clOrdID, rejector);
		if (order != null && order.view().getFields().getExDestination() != null && fields.getExDestination() == null) {
			passThroughService.requestCancelOfPassthrough(order);
		}
		super.handleReplaceRequest(orderID, origClOrdID, clOrdID, fields);
	}
}
