package broke.fix.misc;

import broke.fix.Order;

/**
 * if not supporting corrections and busts,
 * or not supporting corrections and bust messages always have the original quantity and price,
 * then this class does not need to be used
 */
public class Execution {
	private Order order;
	private long qty;
	private double price;

	public Execution init(Order order, long qty, double price) {
		this.order = order;
		this.qty = qty;
		this.price = price;
		return this;
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

	public Order getOrder() {
		return order;
	}

	public long getQty() {
		return qty;
	}

	public double getPrice() {
		return price;
	}

	public static class RestatingExecution extends Execution {
		@Override
		public void bust() {
			//order.replace(order.view().getFields() with orderqQty decreased by qty); //TODO
			super.bust();
		}
	}
}
