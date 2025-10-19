package broke.fix;

import broke.fix.dto.ExecType;
import broke.fix.misc.FixFields;
import broke.fix.misc.IncomingContext;
import broke.fix.misc.IdGenerator;
import broke.fix.misc.NotEnoughQtyException;
import broke.fix.misc.OrderListener;
import broke.fix.misc.OrderCompositeListener;

import java.util.ArrayList;
import java.util.Collection;

import static broke.fix.dto.ExecType.New;
import static broke.fix.dto.ExecType.Restated;

import static java.util.Collections.addAll;
import static java.util.Collections.unmodifiableCollection;

/*
 * When an order could have child orders, its Order object should be wrapped by an instance of this class.
 *
 * That's the major difference between this and the standard composite pattern:
 *  OrderComposite composes not only an OrderComponent for each child order, but also a single extra Order object for its own order-nature.
 */
public class OrderComposite<F extends FixFields> extends OrderComponent<F, OrderComposite<F>> {
	public final Collection<OrderListener<F, OrderComposite<F>>> listeners = new ArrayList<>(); //TODO confirm no garbage, Order, here, next line
	private final Collection<OrderComponent> children = new ArrayList<>();
	private final OrderCompositeListener<F> orderCompositeListener = new OrderCompositeListener<>(this);
	private final IncomingContext incoming;
	private Order<F> order;
	private long workingQtyOfChildren;

	public OrderComposite(IncomingContext incoming) {
		this.incoming = incoming;
	}

	public OrderComposite<F> init(Order<F> order, OrderListener<F, OrderComposite<F>>... listeners) {
		this.order = order;
		this.listeners.clear();
		if (listeners != null) {
			addAll(this.listeners, listeners);
		}
		order.listeners.add(orderCompositeListener);
		children.clear();
		return this;
	}

	public void requestCancel(CharSequence clOrdID) {
		order.cancelRequest.init(clOrdID);
		for (OrderComponent child : children) {
			if (child.getWorkingQty() > 0) {
				child.requestCancel();
			}
		}
		//see addWorkingQtyChange() below for after child requests complete
	}

	public void requestReplace(CharSequence clOrdID, F newFields) {
		order.replaceRequest.init(clOrdID, newFields);
		for (OrderComponent child : children) {
			if (child.getWorkingQty() > 0) {
				child.requestCancel();
			}
		}
		//see addWorkingQtyChange() below for after child requests complete
	}

	public void addChild(OrderComponent child) throws NotEnoughQtyException {
		if (child.getWorkingQty() > getAvailableQty()) { //callers should check this
			throw new NotEnoughQtyException();
		}
		if (child.view().getParent() != null) {
			child.view().getParent().removeChild(this);
		}
		child.setParent(this);
		children.add(child);
		workingQtyOfChildren += child.getWorkingQty();
	}

	public void removeChild(OrderComponent child) {
		child.setParent(null);
		children.remove(child);
		addWorkingQtyChange(-child.getWorkingQty());
	}

	public long getAvailableQty() {
		return order.view().getLeavesQty() - workingQtyOfChildren;
	}

	public Collection getChildren() {
		return unmodifiableCollection(children);
	}

	protected void addWorkingQtyChange(long qtyChange) {
		this.workingQtyOfChildren += qtyChange;
		if (order.cancelRequest.isPending() && workingQtyOfChildren <= 0) {
			order.cancel();
		} else if (order.replaceRequest.isPending() && workingQtyOfChildren <= 0) {
			order.replaceRequest.accept();
		}
	}

	protected void fill(long qty, double px) {
		order.fill(qty, px);
	}

	//TODO something better for this method and the next?
	public CharSequence getReplaceClOrdID() {
		return order.replaceRequest.getClOrdID();
	}

	public CharSequence getCancelClOrdID() {
		return order.cancelRequest.getClOrdID();
	}

	@Override
	protected void setParent(OrderComposite parent) {
		order.setParent(parent);
	}

	@Override
	public long getWorkingQty() {
		return order.getWorkingQty();
	}

	@Override
	public void requestCancel() {
		requestCancel(null);
	}
	
	@Override
	public void requestReplace(F newFields) {
		requestReplace(null, newFields);
	}
	
	@Override
	public Collection<OrderListener<F, OrderComposite<F>>> listeners() {
		return listeners;
	}

	@Override
	public Order.View view() {
		return order.view();
	}
}
