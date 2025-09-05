package broke.fix;

import broke.fix.misc.OrdStatus;
import broke.fix.misc.OrderListener;
import broke.fix.Order.View;
import java.util.Collection;

public abstract class Member<F, H extends Member<F, H>> {
	protected Parental parent;
	protected long orderTime;

	protected abstract void requestCancel();
	protected abstract long getWorkingQty();
	protected abstract void notifyParentOfWorkingQtyChange();

	public abstract View view();
}
