package broke.fix.test;

import broke.fix.Order;
import broke.fix.OrderComposite;
import broke.fix.service.DownstreamHandler;
import broke.fix.misc.IncomingContext;
import broke.fix.misc.IdGenerator;
import broke.fix.misc.OrderListener;
import broke.fix.misc.OrderQtyValidator;
import broke.fix.misc.OrderRepository;
import broke.fix.misc.FixRepository;
import broke.fix.misc.SimplePool;
import broke.fix.misc.Validator;
import broke.fix.service.UpstreamHandler;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import static java.util.Arrays.asList;

public class FixTestBase {
	IdGenerator idgen = new IdGenerator();
	IncomingContext incoming = new IncomingContext();
	Collection<Validator<Fields.Upstream>> validators = asList(new OrderQtyValidator());
	SimplePool<Fields.Upstream> fieldsPool = new SimplePool<>(new ArrayDeque<>(), ()->new Fields.Upstream(), 10);
	SimplePool parentalPool = new SimplePool<>(new ArrayDeque<>(), ()->new OrderComposite(incoming), 10);
	OrderRepository sharedOrderRepo = new SimpleOrderRepository();
	FixRepository parentalRepo = new FixRepository<>(fieldsPool, parentalPool, new HashMap(), new HashMap(), sharedOrderRepo);
	SimplePool orderPool = new SimplePool<>(new ArrayDeque<>(), ()->new Order(incoming, idgen), 10);
	FixRepository childOrderRepo = new FixRepository<>(fieldsPool, orderPool, new HashMap(), new HashMap(), sharedOrderRepo);
	UpstreamPublisher toUpstream = new UpstreamPublisher();
	UpstreamHandler fromUpstream = new UpstreamHandler(incoming, idgen, toUpstream, new HashSet<>(), parentalRepo, orderPool, validators);
	DownstreamHandler fromDownstream = new DownstreamHandler(incoming, childOrderRepo);

	Message lastFromUpstream() {
		Message e = toUpstream.queue.pop();
		System.err.println(e);
		return e;
	}
}
