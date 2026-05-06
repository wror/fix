package broke.fix;

import static java.lang.Long.max;

import broke.fix.misc.FixFields;
import broke.fix.misc.IncomingContext;
import broke.fix.misc.OrderListener;

//with none of the features of CompositeOrder and SendableOrder
public final class SimpleOrder<F extends FixFields> extends Order<F> {
	@SafeVarargs
	public SimpleOrder(IncomingContext context, F fields, OrderListener<SendableOrder<F>, F>... listeners) {
		super(context, fields, safeAsList(listeners));
		endTransaction(l->l.onNewRequest(this, null)); //call listeners without a Request
	}

	@Override
	public long getWorkingQty() {
		return max(0, getLeavesQty());
	}

	public final void fill(final long qty, final double px) {
		fill(getCategory(), qty, px);
	}
}
