package broke.fix.test;

import broke.fix.Order;
import broke.fix.OrderComposite;
import broke.fix.dto.CxlRejReason;
import broke.fix.dto.CxlRejResponseTo;
import broke.fix.dto.ExecInst;
import broke.fix.dto.ExecType;
import broke.fix.dto.MessageHeaderDecoder;
import broke.fix.dto.MessageHeaderEncoder;
import broke.fix.dto.NewOrderSingleDecoder;
import broke.fix.dto.NewOrderSingleEncoder;
import broke.fix.dto.OrdStatus;
import broke.fix.misc.FixFields;
import broke.fix.misc.IncomingContext;
import broke.fix.misc.IdGenerator;
import broke.fix.misc.OrderListener;
import broke.fix.misc.OrderQtyValidator;
import broke.fix.misc.FixRepository;
import broke.fix.misc.SimplePool;
import broke.fix.misc.Validator;
import broke.fix.service.UpstreamHandler;

import org.junit.jupiter.api.Test;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import org.agrona.ExpandableDirectByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static java.util.Arrays.asList;

public class SbeExampleTest {
	IdGenerator idgen = new IdGenerator();
	IncomingContext incoming = new IncomingContext();
	record Message(ExecType execType, CxlRejResponseTo rejType, OrdStatus ordStatus, long cumQty, long leavesQty, long lastShares, CharSequence clOrdID, CharSequence origClOrdID) {
		Message(CxlRejResponseTo rejType, OrdStatus ordStatus, CharSequence clOrdID, CharSequence origClOrdID) {
			this(null, rejType, ordStatus, -1, -1, -1, clOrdID, origClOrdID);
		}

		Message(ExecType execType, OrdStatus ordStatus, long cumQty, long leavesQty, long lastShares, CharSequence clOrdID, CharSequence origClOrdID) {
			this(execType, null, ordStatus, cumQty, leavesQty, lastShares, clOrdID, origClOrdID);
		}
	}
	class UpstreamPublisher implements OrderListener<UpstreamFields, OrderComposite<UpstreamFields>> {
		ArrayDeque<Message> queue = new ArrayDeque<>();
		
		@Override
		public void onExecutionReport(OrderComposite<UpstreamFields> order, ExecType execType, long qty, double px) {
			Order.View v = order.view();
			queue.add(new Message(execType, v.getOrdStatus(), v.getCumQty(), v.getLeavesQty(), qty, v.getClOrdID(), null));
		}

		@Override
		public void onCancelReject(OrderComposite<UpstreamFields> order, CxlRejReason rejectReason) {
			Order.View v = order.view();
			queue.add(new Message(CxlRejResponseTo.Cancel, v.getOrdStatus(), order.getCancelClOrdID(), v.getClOrdID()));
		}

		@Override
		public void onReplaceReject(OrderComposite<UpstreamFields> order, CxlRejReason rejectReason) {
			Order.View v = order.view();
			queue.add(new Message(CxlRejResponseTo.Replace, v.getOrdStatus(), order.getReplaceClOrdID(), v.getClOrdID()));
		}
	}

	static class UpstreamFields implements FixFields {
		double price;
		long orderQty;
		public long getOrderQty() { return orderQty; }
		public double getPrice() { return price; }
	}
	Collection<Validator<UpstreamFields>> validators = asList(new OrderQtyValidator());
	SimplePool parentalPool = new SimplePool<>(new ArrayDeque<>(), ()->new OrderComposite(incoming), 10);
	FixRepository parentalRepo = new FixRepository<>(parentalPool, new HashMap(), new HashMap(), new FixTest.SimpleOrderRepository());
	SimplePool orderPool = new SimplePool<>(new ArrayDeque<>(), ()->new Order(incoming, idgen), 10);
	UpstreamPublisher toUpstream = new UpstreamPublisher();
	UpstreamHandler fromUpstream = new UpstreamHandler(incoming, idgen, toUpstream, new HashSet<>(), parentalRepo, orderPool, validators);

	@Test
	public void accept() {
		ExpandableDirectByteBuffer buffer = new ExpandableDirectByteBuffer();
		NewOrderSingleEncoder en = new NewOrderSingleEncoder().wrapAndApplyHeader(buffer, 0, new MessageHeaderEncoder());
		NewOrderSingleDecoder de = new NewOrderSingleDecoder().wrapAndApplyHeader(buffer, 0, new MessageHeaderDecoder());

		en.orderQty(100);
		en.price(1.2);
		
		UpstreamFields f = new UpstreamFields();
		f.orderQty = de.orderQty();
		f.price = de.price();
		fromUpstream.handleNewRequest("c1", f, 1);
		assertEquals(OrdStatus.New, lastFromUpstream().ordStatus());
	}

	//TODO really interesting would be a realistic outgoing message and jmh it

	private Message lastFromUpstream() {
		Message e = toUpstream.queue.pop();
		System.err.println(e);
		return e;
	}
}
