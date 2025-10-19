package broke.fix.test;

import broke.fix.dto.OrdStatus;
import broke.fix.dto.MessageHeaderDecoder;
import broke.fix.dto.MessageHeaderEncoder;
import broke.fix.dto.NewOrderSingleDecoder;
import broke.fix.dto.NewOrderSingleEncoder;

import org.junit.jupiter.api.Test;

import org.agrona.ExpandableDirectByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static java.util.Arrays.asList;

public class SbeExampleTest extends FixTestBase {
	@Test
	public void accept() {
		ExpandableDirectByteBuffer buffer = new ExpandableDirectByteBuffer();
		NewOrderSingleEncoder en = new NewOrderSingleEncoder().wrapAndApplyHeader(buffer, 0, new MessageHeaderEncoder());
		NewOrderSingleDecoder de = new NewOrderSingleDecoder().wrapAndApplyHeader(buffer, 0, new MessageHeaderDecoder());

		en.orderQty(100);
		en.price(1.2);
		
		Fields.Upstream f = fieldsPool.acquire();
		f.orderQty = de.orderQty();
		f.price = de.price();
		fromUpstream.handleNewRequest("c1", f, 1);
		assertEquals(OrdStatus.New, lastFromUpstream().ordStatus());
	}

	//TODO really interesting would be a realistic outgoing message and jmh it
}
