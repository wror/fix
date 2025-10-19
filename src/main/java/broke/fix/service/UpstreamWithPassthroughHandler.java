package broke.fix.service;

import broke.fix.Order;
import broke.fix.OrderComposite;
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

public class UpstreamWithPassthroughHandler<F extends FixFields> extends UpstreamHandler<F> {
	private final PassthroughFactory passThroughFactory;

	@Inject
	public UpstreamWithPassthroughHandler(PassthroughFactory passThroughFactory, IncomingContext incoming, IdGenerator idgen, OrderListener<F, OrderComposite<F>> publisher, Set<CharSequence> clOrdIDs, FixRepository<F, OrderComposite<F>> repo, SimplePool<Order<F>> pool, Collection<Validator<F>> validators) {
		super(incoming, idgen, publisher, clOrdIDs, repo, pool, validators);
		this.passThroughFactory = passThroughFactory;
	}
	
	@Override
	protected void handleNewOrder(Order<F> order, OrderComposite<F> composite) {
		if (order.view().getFields().getExDestination() == null) {
			super.handleNewOrder(order, composite);
		} else {
			passThroughFactory.slicePassthrough(order, composite, order.view().getFields());
		}
	}
}
