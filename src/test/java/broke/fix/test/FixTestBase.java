package broke.fix.test;

import broke.fix.downstream.DownstreamHandler;
import broke.fix.downstream.DownstreamRepository;
import broke.fix.misc.IncomingContext;
import broke.fix.misc.OrderQtyValidator;
import broke.fix.misc.Validator;
import broke.fix.upstream.UpstreamHandler;
import broke.fix.upstream.UpstreamRepository;

import java.util.Collection;
import static java.util.Arrays.asList;

import java.time.Clock;

public class FixTestBase {
	IncomingContext incoming = new IncomingContext(Clock.systemDefaultZone(), ()->System.nanoTime());
	Collection<Validator<Fields.Upstream>> validators = asList(new OrderQtyValidator<Fields.Upstream>());
	UpstreamRepository<Fields.Upstream> parentalRepo = new UpstreamRepository<>();
	DownstreamRepository childOrderRepo = new DownstreamRepository();
	UpstreamPublisher toUpstream = new UpstreamPublisher();
	UpstreamHandler<Fields.Upstream> fromUpstream = new UpstreamHandler<>(incoming, toUpstream, parentalRepo, validators);
	DownstreamHandler fromDownstream = new DownstreamHandler(incoming, childOrderRepo);

	Message lastFromUpstream() {
		Message e = null;
		while (!toUpstream.queue.isEmpty()) {
			e = toUpstream.queue.pop();
		}
		System.err.println(e);
		return e;
	}
}
