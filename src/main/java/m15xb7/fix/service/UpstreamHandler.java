package m15xb7.fix.service;

import m15xb7.fix.Order;
import m15xb7.fix.ParentOrder;
import m15xb7.fix.misc.IdGenerator;
import m15xb7.fix.misc.SimplePool;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.HashMap;

public class UpstreamHandler<R> {
	private Map<CharSequence, Order> orderByClOrdID = new HashMap<>(); //TODO dependency injection or something
	private Map<CharSequence, Order> orderByOrderID = new HashMap<>();
	private IdGenerator idgen = new IdGenerator();
	private final SimplePool<Order<R, R>> pool = new SimplePool<>(new ArrayDeque<>(), Order::new, 10);
	private final SimplePool<ParentOrder<R>> parentPool = new SimplePool<>(new ArrayDeque<>(), ParentOrder::new, 10);

	public void handleNew(CharSequence orderID, CharSequence clOrdID, R fields, long orderQty, long transactTime) {
		idgen.incomingTransactTime = transactTime;
		Order order = pool.acquire().init(idgen, null, fields, orderQty);
		ParentOrder parentOrder = parentPool.acquire().init(order); //TODO call something to put it
		orderByClOrdID.put(clOrdID, order);
	}

//TODO support OrigOrdModTime https://www.onixs.biz/fix-dictionary/4.4/tagNum_586.html

	public void handleReplace(CharSequence orderID, CharSequence clOrdID, R fields, long orderQty) {
		Order order = getOrder(orderID, clOrdID);
		if (order != null) {
			//TODO reject
		}
		order.replaceRequest.init(clOrdID, fields, orderQty);
	}

	public void handleCancel(CharSequence orderID, CharSequence clOrdID) {
		Order order = getOrder(orderID, clOrdID);
		if (order != null) {
			//TODO reject
		}
		order.cancelRequest.init(clOrdID);
	}

	public Order getOrder(CharSequence orderID, CharSequence clOrdID) {
		if (orderID != null) {
			Order order = orderByOrderID.get(orderID);
			if (order != null) {
				return order;
			}
		}
		if (clOrdID != null) {
			Order order = orderByClOrdID.get(clOrdID);
			if (clOrdID != null) {
				return order;
			}
		}
		return null;
	}
}
