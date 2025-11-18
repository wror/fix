package broke.fix.misc;

import broke.fix.Order;

public class OrderQtyValidator implements Validator {
	@Override
	public CharSequence getInvalidMessage(FixFields fields, Order.View order) {
		if (!(fields.getOrderQty() > 0)) {
			return "orderQty must be > 0";
		}

		long previousOrderQty = order.isPending() ? 0 : order.getFields().getOrderQty();
		if (order.getParent() != null && fields.getOrderQty() - previousOrderQty > order.getParent().getAvailableQty()) {
			return "orderQty must not be greater than parent's available quantity";
		}

		return null;
	}
}
