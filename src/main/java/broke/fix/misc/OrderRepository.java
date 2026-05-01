package broke.fix.misc;

import broke.fix.Order;

public interface OrderRepository<F extends FixFields> {
	void addOrder(Order<F> order);
	void removeOrder(Order<F> order);
	Order<F> getOrder(long internalOrderID);
}
