package broke.fix.test;

import broke.fix.misc.FixFields;

class Fields {
	static class Upstream implements FixFields {
		double price;
		long orderQty;
		public long getOrderQty() { return orderQty; }
		public double getPrice() { return price; }
	}

	static class Downstream implements FixFields {
		long orderQty;
		public long getOrderQty() { return orderQty; }
	}
}
