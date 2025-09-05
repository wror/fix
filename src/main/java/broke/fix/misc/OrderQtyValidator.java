package broke.fix.misc;

import broke.fix.Order;

public class OrderQtyValidator implements Validator {
	@Override
	public String getInvalidMessage(long orderQty, Object fields, Order.View order) {
		//TODO when received from upstream, how to populate OrdRejReason.IncorrectQuantity ?
		if (!(orderQty > 0)) {
			return "orderQty must be > 0";
		}

		long previousOrderQty = order.isPending() ? 0 : order.getOrderQty();
		if (order.getParent() != null && orderQty - previousOrderQty > order.getParent().getAvailableQty()) {
			return "orderQty must not be greater than parent's available quantity";
		}

		return null;
	}
}
