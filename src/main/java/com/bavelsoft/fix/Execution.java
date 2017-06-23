package com.bavelsoft.fix;

public class Execution {
        private long qty;
        private double price;
	private Order order;

        public Execution(Order order, long qty, double price) {
		this.order = order;
                this.qty = qty;
                this.price = price;
        }

        public void cancel() {
                qty = -qty;
                order.fill(this);
        }

        public void correct(long qty, double price) {
                cancel();
                this.qty = qty;
                this.price = price;
                order.fill(this);
        }

	public long getQty() {
		return qty;
	}

	public double getNewAvgPx(Order order) {
		double value = qty * price;
		double orderValue = order.getCumQty()*order.getAvgPx();
		return (orderValue + value) / (order.getCumQty() + qty);
	}
}
