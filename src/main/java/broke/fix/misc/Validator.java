package broke.fix.misc;

import broke.fix.Order;

public interface Validator<F> {
	String getInvalidMessage(long orderQty, F fields, Order<F>.View order);
}
