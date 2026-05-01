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

public class FixTestBase {
	IncomingContext incoming = new IncomingContext();
	Collection<Validator<Fields.Upstream>> validators = asList(new OrderQtyValidator<Fields.Upstream>());
	UpstreamRepository parentalRepo = new UpstreamRepository();
	DownstreamRepository childOrderRepo = new DownstreamRepository();
	UpstreamPublisher toUpstream = new UpstreamPublisher();
	UpstreamHandler fromUpstream = new UpstreamHandler(incoming, toUpstream, parentalRepo, validators);
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
