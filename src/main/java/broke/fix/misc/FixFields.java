package broke.fix.misc;

import broke.fix.dto.ExecInst;
import broke.fix.dto.OrdType;
import broke.fix.dto.PriceType;
import broke.fix.dto.Side;

public interface FixFields extends Cloneable {
	long getOrderQty();
	default OrdType getOrdType() { return OrdType.Market; }
	default Side getSide() { return null; }
	default double getPrice() { return Double.NaN; }
	default PriceType getPriceType() { return PriceType.PerUnit; }
	default long getOrigOrdModTime() { return 0; }
	default boolean hasExecInst(ExecInst i) { return false; }
	default CharSequence getExDestination() { return null; }
	default RelativePrice relativePrice() { return new RelativePrice(this); }
	default boolean isPriceLimited() {
		return getOrdType() == OrdType.Limit || getOrdType() == OrdType.StopLimit;
	}
	default <F extends FixFields> boolean areMoreRestrictiveThan(F fields) {
		return isPriceLimited() && fields.relativePrice().isLessAggressiveThan(getPrice());
	}
}
