package broke.fix.downstream;

import broke.fix.CompositeOrder;
import broke.fix.SendableOrder;
import broke.fix.misc.FixFields;
import broke.fix.misc.IncomingContext;
import broke.fix.misc.OrderListener;
import broke.fix.request.NewRequest;

import java.time.InstantSource;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DownstreamFactory<F extends FixFields> {
	private final static Logger log = LogManager.getLogger();
	private final DownstreamRepository<F> repo;
	private final OrderListener<SendableOrder<F>> publisher;
	private final IncomingContext context = new IncomingContext();
	private InstantSource clock;

	@Inject
	public DownstreamFactory(DownstreamRepository<F> repo, OrderListener<SendableOrder<F>> publisher, InstantSource clock) {
		this.repo = repo;
		this.publisher = publisher;
		this.clock = clock;
	}

	public void slice(CompositeOrder<F> parent, F fields) {
		context.transactTime = clock.millis();
		SendableOrder<F> childOrder = new SendableOrder<>(context, fields, publisher);
		parent.addChild(childOrder);
		repo.addNew(new NewRequest<>(childOrder));
	}
}
