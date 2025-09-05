package broke.fix.misc;

import broke.fix.OrderComponent;

public interface OrderRepository<F, H extends OrderComponent<F, H>> {
	void addOrder(H order);
	void removeOrder(H order);
	H getOrder(long internalOrderID);
}
