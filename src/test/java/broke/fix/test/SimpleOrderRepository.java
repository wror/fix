package broke.fix.test;

import broke.fix.Order;
import broke.fix.misc.OrderRepository;

import java.util.HashMap;

class SimpleOrderRepository implements OrderRepository {
	private HashMap<Long, Order> map = new HashMap<>();

	@Override
	public void addOrder(Order order) {
		map.put(order.getInternalOrderID(), order);
	}

	@Override
	public void removeOrder(Order order) {
		map.remove(order.getInternalOrderID());
	}

	@Override
	public Order getOrder(long orderID) {
		return map.get(orderID);
	}
}
