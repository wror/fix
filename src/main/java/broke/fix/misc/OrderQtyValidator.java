package broke.fix.misc;

import broke.fix.Order;
import broke.fix.dto.OrdStatus;

public class OrderQtyValidator<F extends FixFields> implements Validator<F> {
	@Override
	public CharSequence getInvalidMessage(FixFields fields, Order order) {
		if (!(fields.getOrderQty() > 0)) {
			return "orderQty must be > 0";
		}

		long previousOrderQty = order.getOrdStatus() == OrdStatus.PendingNew ? 0 : order.getFields().getOrderQty();
		if (order.getParent() != null && fields.getOrderQty() - previousOrderQty > order.getParent().getAvailableQty()) {
			return "orderQty must not be greater than parent's available quantity";
		}

		return null;
	}
}
