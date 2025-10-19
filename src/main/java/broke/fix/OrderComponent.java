package broke.fix;

import broke.fix.dto.OrdStatus;
import broke.fix.misc.OrderListener;
import broke.fix.Order.View;
import broke.fix.OrderComposite;
import java.util.Collection;

/*
 * Most code that deals with orders should use this class, and not the more sharply named Order class.
 *
 * For example, OrderListener and OrderRepository use this class, so they can be used for both upstream and downstream orders.
 */
public abstract class OrderComponent<F, H extends OrderComponent<F, H>> {
	protected abstract void setParent(OrderComposite parent);
	public abstract void requestCancel();
	public abstract void requestReplace(F newFields);
	public abstract long getWorkingQty();
	public abstract Collection<OrderListener<F, H>> listeners();
	public abstract View view();
}
