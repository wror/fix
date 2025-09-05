package broke.fix;

import broke.fix.misc.OrdStatus;
import broke.fix.misc.OrderListener;
import broke.fix.Order.View;
import broke.fix.Parental;
import java.util.Collection;

public abstract class Member<F, H extends Member<F, H>> {
	protected abstract void requestCancel();
	protected abstract void setParent(Parental parent);

	public abstract long getWorkingQty();
	public abstract Collection<OrderListener<F, H>> listeners();
	public abstract View view();
}
