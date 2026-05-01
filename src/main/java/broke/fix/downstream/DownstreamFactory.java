package broke.fix.downstream;

import broke.fix.CompositeOrder;
import broke.fix.DownstreamOrder;
import broke.fix.misc.FixFields;
import broke.fix.misc.IncomingContext;
import broke.fix.misc.OrderListener;

import java.time.InstantSource;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DownstreamFactory<F extends FixFields> {
	private final static Logger log = LogManager.getLogger();
	private final DownstreamRepository repo;
	private final OrderListener<DownstreamOrder<F>> publisher;
	private final IncomingContext context = new IncomingContext();
	private InstantSource clock;

	@Inject
	public DownstreamFactory(DownstreamRepository repo, OrderListener<DownstreamOrder<F>> publisher, InstantSource clock) {
		this.repo = repo;
		this.publisher = publisher;
		this.clock = clock;
	}

	public void slice(CompositeOrder<F> parent, F fields) {
		context.transactTime = clock.millis();
		DownstreamOrder<F> childOrder = new DownstreamOrder<>(context, fields, publisher);
		parent.addChild(childOrder);
		repo.addNew(childOrder.requestNew());
	}
}
