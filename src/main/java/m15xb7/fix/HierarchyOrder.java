package m15xb7.fix;

import m15xb7.fix.misc.OrdStatus;
import java.util.Collection;

public abstract class HierarchyOrder<F, H extends HierarchyOrder<F, H>> {
	ParentOrder parent;
	long orderTime;

	abstract void requestCancel();
	abstract long getWorkingQty();
	abstract void notifyParentOfWorkingQtyChange();
}
