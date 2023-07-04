package com.bavelsoft.fix;

import com.bavelsoft.fix.Order;

/**
 * if not supporting corrections, and cancel messages always have the original quantity and price,
 * then this class does not need to be used
 */
public class Execution {
	private Order order;
        private long qty;
        private double price;

        public void init(Order order, long qty, double price) {
		this.order = order;
                this.qty = qty;
                this.price = price;
        }

        public void bust() {
		qty = -qty;
		//TODO some venues want to call order.cancel(qty) here too
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
}
