package broke.fix.test;

import broke.fix.Order;
import broke.fix.Parental;
import broke.fix.service.DownstreamHandler;
import broke.fix.misc.CxlRejReason ;
import broke.fix.misc.CxlRejResponseTo;
import broke.fix.misc.ExecType;
import broke.fix.misc.IncomingContext;
import broke.fix.misc.IdGenerator;
import broke.fix.misc.OrderListener;
import broke.fix.misc.OrderQtyValidator;
import broke.fix.misc.OrdStatus;
import broke.fix.misc.SimpleOrderRepo;
import broke.fix.misc.SimplePool;
import broke.fix.misc.Validator;
import broke.fix.service.UpstreamHandler;

import org.junit.jupiter.api.Test;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static java.util.Arrays.asList;

public class FixTest {
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
	class UpstreamPublisher implements OrderListener<UpstreamFields, Parental<UpstreamFields>> {
		ArrayDeque<Message> queue = new ArrayDeque<>();
		
		@Override
		public void onFill(Parental<UpstreamFields> order, long qty, double px) {
			Order.View v = order.view();
			queue.add(new Message(ExecType.Trade, v.getOrdStatus(), v.getCumQty(), v.getLeavesQty(), qty, v.getClOrdID(), v.getOrigClOrdID()));
		}

		@Override
		public void onNonFillExecutionReport(Parental<UpstreamFields> order, ExecType execType) {
			Order.View v = order.view();
			queue.add(new Message(execType, v.getOrdStatus(), v.getCumQty(), v.getLeavesQty(), -1, v.getClOrdID(), v.getOrigClOrdID()));
		}

		@Override
		public void onCancelReject(Parental<UpstreamFields> order, CxlRejReason rejectReason) {
			Order.View v = order.view();
			queue.add(new Message(CxlRejResponseTo.Cancel, v.getOrdStatus(), v.getClOrdID(), v.getOrigClOrdID()));
		}

		@Override
		public void onReplaceReject(Parental<UpstreamFields> order, CxlRejReason rejectReason) {
			Order.View v = order.view();
			queue.add(new Message(CxlRejResponseTo.Replace, v.getOrdStatus(), v.getClOrdID(), v.getOrigClOrdID()));
		}
	}

	static class UpstreamFields {
		double price;
	}
	static class DownstreamFields {
	}
	class PriceValidator implements Validator<UpstreamFields> {
		@Override
		public String getInvalidMessage(long orderQty, UpstreamFields fields, Order<UpstreamFields>.View view) {
			UpstreamFields f = view.getFields();
			if (!(fields.price > 0)) {
				return "price must be greater than 0";
			}
			return null;
		}
	}
	Collection<Validator<UpstreamFields>> validators = asList(new OrderQtyValidator(), new PriceValidator());
	SimpleOrderRepo parentOrderRepo = new SimpleOrderRepo<>(new ArrayDeque<>(), ()->new Parental(incoming), new HashSet<>(), new HashMap<>(), new HashMap<>(), new HashMap<>());
	SimpleOrderRepo childOrderRepo = new SimpleOrderRepo<>(new ArrayDeque<>(), ()->new Order(incoming, idgen), null, new HashMap<>(), new HashMap<>(), new HashMap<>());
	SimplePool simplePool = new SimplePool<>(new ArrayDeque<>(), ()->new Order(incoming, idgen), 10);
	UpstreamPublisher toUpstream = new UpstreamPublisher();
	UpstreamHandler fromUpstream = new UpstreamHandler(incoming, idgen, toUpstream, parentOrderRepo, simplePool, validators);
	DownstreamHandler fromDownstream = new DownstreamHandler(incoming, idgen, childOrderRepo);

	@Test
	public void ack() {
		UpstreamFields f = new UpstreamFields();
		f.price = 1.2;
		fromUpstream.handleNew("c1", 10, f, 1);
		assertEquals(OrdStatus.New, lastFromUpstream().ordStatus());
	}

	private Message lastFromUpstream() {
		Message e = toUpstream.queue.pop();
		System.err.println(e);
		return e;
	}
}
