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

import static org.apache.logging.log4j.util.Unbox.box;

public class SimpleOrderRepo<F, H extends Member<F, H>> {
	private final static Logger log = LogManager.getLogger();
	private final SimplePool<H> pool;
	private final Map<CharSequence, H> orderByClOrdID;
	private final Map<CharSequence, H> orderByDownstreamOrderID;
	private final Map<Long, H> orderByOrderID; //TODO how to enable any of https://github.com/carrotsearch/hppc/blob/master/ALTERNATIVES.txt

	public SimpleOrderRepo(SimplePool<H> pool, Map<CharSequence, H> orderByClOrdID, Map<CharSequence, H> orderByDownstreamOrderID, Map<Long, H> orderByOrderID) {
		this.pool = pool;
		this.orderByClOrdID = orderByClOrdID;
		this.orderByDownstreamOrderID = orderByDownstreamOrderID;
		this.orderByOrderID = orderByOrderID;
	}

	public H acquire() {
		return pool.acquire();
	}

	public void release(H order) {
		pool.release(order);
	}

	public void addOrder(H order) {
		order.listeners().add(cleanupListener);
		orderByClOrdID.put(order.view().getClOrdID(), order);
		orderByOrderID.put(order.view().getOrderID(), order);
		if (order.view().getDownstreamOrderID() != null) {
			orderByDownstreamOrderID.put(order.view().getDownstreamOrderID(), order);
		}
	}

	public void updateClOrdID(CharSequence origClOrdID, CharSequence clOrdID, H order) {
		orderByClOrdID.remove(origClOrdID);
		orderByClOrdID.put(clOrdID, order);
	}
	
	OrderListener<F, H> cleanupListener = new OrderListener<F, H>() {
		@Override
		public void onExecutionReport(H order, ExecType execType, long qty, double px) {
			if (order.getWorkingQty() <= 0) {
				log.info("Order {} recycled", box(order.view().getOrderID()));
				removeOrder(order);
			}
		}
	};

	public void removeOrder(H order) {
		orderByClOrdID.remove(order.view().getClOrdID());
		orderByOrderID.remove(order.view().getOrderID());
		if (order.view().getDownstreamOrderID() != null) {
			orderByDownstreamOrderID.remove(order.view().getDownstreamOrderID());
		}
		pool.release(order);
	}

	public H getOrder(long orderID) {
		return orderByOrderID.get(orderID);
	}

	public H getOrder(long orderID, CharSequence clOrdID) {
		H order = orderByOrderID.get(orderID);
		if (order != null) {
			return order;
		}
		return getOrderByClOrdIdOrLog(box(orderID), clOrdID);
	}

	public H getOrder(CharSequence orderID, CharSequence clOrdID) {
		if (orderID != null) { //Will be null for the first message from downstream for each order
			H order = orderByDownstreamOrderID.get(orderID);
			if (order != null) {
				return order;
			}
		}
		return getOrderByClOrdIdOrLog(orderID, clOrdID);
	}

	private H getOrderByClOrdIdOrLog(Object orderID, CharSequence clOrdID) {
		H order = orderByClOrdID.get(clOrdID);
		if (order != null) {
			return order;
		}
		log.warn("Couldn't find order {}/{}", orderID, clOrdID);
		return null;
	}
}
