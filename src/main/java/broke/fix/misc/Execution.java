package broke.fix.misc;

import broke.fix.SendableOrder;

/**
 * if not supporting corrections and busts,
 * or if not supporting corrections, and bust messages always have the original quantity and price,
 * then this class does not need to be used
 */
public class Execution {
	private SendableOrder<?> order;
	private long qty;
	private double price;

	public Execution(SendableOrder<?> order, long qty, double price) {
		this.order = order;
		this.qty = qty;
		this.price = price;
		order.fill(qty, price);
	}

	public void bust() {
		qty = -qty;
		order.fill(qty, price);
	}

	public void correct(long qty, double price) {
		bust();
		this.qty = qty;
		this.price = price;
		order.fill(qty, price);
	}

	public SendableOrder<?> getOrder() {
		return order;
	}

	public long getQty() {
		return qty;
	}

	public double getPrice() {
		return price;
	}

	public static class ReinstatingExecution extends Execution {
		public ReinstatingExecution(SendableOrder<?> order, long qty, double price) {
			super(order, qty, price);
		}

		@Override
		public void bust() {
			RestatableFixFields fields = (RestatableFixFields)getOrder().getFields();
			fields.setOrderQty(fields.getOrderQty() - getQty());
			super.bust();
		}

		interface RestatableFixFields extends FixFields {
			void setOrderQty(long v);
		}
	}
}
