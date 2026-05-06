package broke.fix.downstream;

import broke.fix.CompositeOrder;
import broke.fix.SendableOrder;
import broke.fix.misc.FixFields;
import broke.fix.misc.IncomingContext;
import broke.fix.misc.OrderListener;
import broke.fix.request.NewRequest;

import java.time.InstantSource;

import javax.inject.Inject;

public class DownstreamFactory<F extends FixFields> {
	private final DownstreamRepository repo;
	private final OrderListener<SendableOrder<F>, F> publisher;
	private final IncomingContext context;
	private final InstantSource clock;

	@Inject
	public DownstreamFactory(DownstreamRepository repo, OrderListener<SendableOrder<F>, F> publisher, InstantSource clock) {
		this.repo = repo;
		this.publisher = publisher;
		this.clock = clock;
		this.context = new IncomingContext(clock);
	}

	public void slice(CompositeOrder<F> parent, F fields) {
		context.transactTime = clock.millis();
		SendableOrder<F> childOrder = new SendableOrder<>(context, fields, publisher);
		parent.addChild(childOrder);
		repo.addNew(new NewRequest(childOrder));
	}
}
