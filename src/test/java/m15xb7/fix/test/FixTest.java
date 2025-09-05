package m15xb7.fix.test;

import m15xb7.fix.service.DownstreamHandler;
import m15xb7.fix.service.DownstreamPublisher;
import m15xb7.fix.misc.ExecType;
import m15xb7.fix.service.UpstreamHandler;
import m15xb7.fix.service.UpstreamPublisher;
import org.junit.jupiter.api.Test;

public class FixTest {
	record NewOrderSingle(long orderQty, double price) {}
	record ExecutionReport(ExecType type) {}
	UpstreamHandler fromUpstream = new UpstreamHandler();
	DownstreamHandler fromDownstream = new DownstreamHandler();
	UpstreamPublisher toUpstream = new UpstreamPublisher();
	DownstreamPublisher toDownstream = new DownstreamPublisher();

	@Test
	public void simple() {
//TODO magic!
	}
}
