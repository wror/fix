package broke.fix.misc;

import broke.fix.dto.PriceType;
import broke.fix.dto.Side;

public class RelativePrice {
	private static final double EPSILON = 1e-6;
	private FixFields fields;

	public RelativePrice(FixFields fields) {
		this.fields = fields;
	}

	public boolean isLessAggressiveThan(double p) {
		if (!fields.isPriceLimited()) {
			return false;
		}
		if (Double.isNaN(p)) {
			return true;
		}
		double difference = lowerIsLess() ? p - fields.getPrice() : fields.getPrice() - p;
		return difference > EPSILON;
	}

	public double roundedUnderLimit(double tickSize, double p) {
		if (fields.isPriceLimited()) {
			p = lowerIsLess() ? Math.min(p, fields.getPrice()) : Math.max(p, fields.getPrice()); 
		}
		double ticks = p / tickSize;
		return (lowerIsLess() ? Math.floor(ticks) : Math.ceil(ticks)) * tickSize;
	}
	
	private boolean lowerIsLess() {
		boolean lowerIsLess = fields.getSide() == Side.Buy;
		if (fields.getPriceType() == PriceType.Discount) { //we assume that p is the same price type
			lowerIsLess = !lowerIsLess;
		}
		return lowerIsLess;
	}
}
