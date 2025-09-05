package broke.fix.misc;

import broke.fix.Member;
import broke.fix.Order;
import broke.fix.Parental;

import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.Queue;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SimpleOrderRepo<F, H extends Member<F, H>> {
	private final static Logger log = LogManager.getLogger();
	private final SimplePool<H> pool;
	private final Set<CharSequence> nonOrderClOrdIDs;
	private final Map<CharSequence, H> orderByClOrdID;
	private final Map<CharSequence, H> orderByDownstreamOrderID;
	private final Map<Long, H> orderByOrderID;
	private final OrderListener<F, H>[] listeners;

	public SimpleOrderRepo(Queue<H> queue, Supplier<H> constructor, Set<CharSequence> nonOrderClOrdIDs, Map<CharSequence, H> orderByClOrdID, Map<CharSequence, H> orderByDownstreamOrderID, Map<Long, H> orderByOrderID, OrderListener<F, H>... listeners) {
		this.pool = new SimplePool<>(queue, constructor, 10);
		this.nonOrderClOrdIDs = nonOrderClOrdIDs;
		this.orderByClOrdID = orderByClOrdID;
		this.orderByDownstreamOrderID = orderByDownstreamOrderID;
		this.orderByOrderID = orderByOrderID;
		this.listeners = listeners;
	}

	public boolean isDuplicateClOrdID(CharSequence clOrdID) {
		return orderByClOrdID.containsKey(clOrdID) || nonOrderClOrdIDs.contains(clOrdID);
	}

	public void recordNonOrderClOrdID(CharSequence clOrdID) {
		nonOrderClOrdIDs.add(clOrdID);
	}

	public H acquire() {
		return pool.acquire();
	}

	public void release(H order) {
		pool.release(order);
	}

	public void addOrder(H order) {
		orderByClOrdID.put(order.view().getClOrdID(), order);
		orderByOrderID.put(order.view().getOrderID(), order);
		if (order.view().getDownstreamOrderID() != null) {
			orderByDownstreamOrderID.put(order.view().getDownstreamOrderID(), order);
		}
	}

	//TODO perhaps a listener to call this on terminal orders
	public void removeOrder(H order) {
		orderByClOrdID.remove(order.view().getClOrdID());
		orderByOrderID.remove(order.view().getOrderID());
		pool.release(order);
	}

	public H getOrder(CharSequence orderID, CharSequence clOrdID) {
		if (orderID != null) {
			H order = orderByOrderID.get(orderID);
			if (order != null) {
				return order;
			}
		}
		if (clOrdID != null) {
			H order = orderByClOrdID.get(clOrdID);
			if (clOrdID != null) {
				return order;
			}
		}
		log.warn("Couldn't find order {}/{}", orderID, clOrdID);
		return null;
	}
}
