package broke.fix.test;

import broke.fix.dto.OrdStatus;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FixTest extends FixTestBase {
	@Test
	public void accept() {
		Fields.Upstream f = fieldsPool.acquire();
		f.orderQty = 10;
		f.price = 1.2;
		fromUpstream.handleNewRequest("c1", f, 1);
		assertEquals(OrdStatus.New, lastFromUpstream().ordStatus());
	}

	@Test
	public void reject() {
		Fields.Upstream f = new Fields.Upstream();
		f.orderQty = 0;
		f.price = 1.2;
		fromUpstream.handleNewRequest("c1", f, 1);
		assertEquals(OrdStatus.Rejected, lastFromUpstream().ordStatus());
	}
}
