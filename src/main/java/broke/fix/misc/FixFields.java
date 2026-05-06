package broke.fix.misc;

import broke.fix.dto.ExecInst;
import broke.fix.dto.Side;

public interface FixFields {
	long getOrderQty();
	default Side getSide() { return null; }
	default double getPrice() { return 0; }
	default long getOrigOrdModTime() { return 0; }
	default boolean hasExecInst(ExecInst i) { return false; }
	default CharSequence getExDestination() { return null; }

	//TODO epsilon support
	//TODO discount price support
	//TODO market price handling
	//TODO better naming?
	default boolean isPriceMoreGenerousThan(double p) {
		return getSide() == Side.Buy ? getPrice() > p : getPrice() < p;
	}
	default boolean isPriceLessGenerousThan(double p) {
		return getSide() == Side.Buy ? getPrice() < p : getPrice() > p;
	}
}
