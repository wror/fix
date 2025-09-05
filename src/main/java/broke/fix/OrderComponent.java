package broke.fix;

import broke.fix.dto.OrdStatus;
import broke.fix.misc.OrderListener;
import broke.fix.Order.View;
import broke.fix.OrderComposite;
import java.util.Collection;

public abstract class OrderComponent<F, H extends OrderComponent<F, H>> {
	protected abstract void setParent(OrderComposite parent);
	public abstract void requestCancel();
	public abstract void requestReplace(F newFields);
	public abstract long getWorkingQty();
	public abstract Collection<OrderListener<F, H>> listeners();
	public abstract View view();
}
