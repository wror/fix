package broke.fix.misc;

import broke.fix.Order;

public interface Validator<F extends FixFields> {
	CharSequence getInvalidMessage(F fields, Order<F>.View order);
}
