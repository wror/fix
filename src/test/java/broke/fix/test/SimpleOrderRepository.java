package broke.fix.test;

import broke.fix.OrderComponent;
import broke.fix.misc.OrderRepository;

import java.util.HashMap;

class SimpleOrderRepository implements OrderRepository {
	private HashMap<Long, OrderComponent> map = new HashMap<>();

	@Override
	public void addOrder(OrderComponent order) {
		map.put(order.view().getInternalOrderID(), order);
	}

	@Override
	public void removeOrder(OrderComponent order) {
		map.remove(order.view().getInternalOrderID());
	}

	@Override
	public OrderComponent getOrder(long orderID) {
		return map.get(orderID);
	}
}
