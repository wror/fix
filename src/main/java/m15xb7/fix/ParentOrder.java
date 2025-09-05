package m15xb7.fix;

import m15xb7.fix.misc.ExecType;
import m15xb7.fix.misc.OrderListener;

import java.util.ArrayList;
import java.util.Collection;

import static java.util.Collections.unmodifiableCollection;

/*
 * when an order could have child orders, its Order object should be wrapped by an instance of this class
 */
public class ParentOrder<F> extends HierarchyOrder<F, ParentOrder<F>> {
	Order<F, ?> order;
	public Order.View view;
	private Collection<HierarchyOrder> children;
	private long workingQtyOfChildren;
	private long replaceRequested;

	public ParentOrder<F> init(Order<F, ?> order) {
		this.order = order;
		this.view = order.view;
		order.orderID = order.idgen.getOrderID()+"";
		this.children = new ArrayList<>();
		return this;
	}

	@Override
	long getWorkingQty() {
		return order.getWorkingQty();
	}

	@Override
	void notifyParentOfWorkingQtyChange() {
		order.notifyParentOfWorkingQtyChange();
	}

	@Override
	public void requestCancel() {
		order.requestCancel();
		for (HierarchyOrder child : children) {
			if (child.getWorkingQty() > 0) {
				child.requestCancel();
			}
		}
	}

	public void requestReplace(F newFields, long newOrderQty) {
		this.replaceRequested = order.idgen.getTime();
		this.order.requestReplace(newFields, newOrderQty);
		for (HierarchyOrder child : children) {
			//TODO way to compare F to check their price or quantity or whatever? and instead update their orderTime to replaceRequested
			child.requestCancel();
		}
	}

	public void addChild(HierarchyOrder child) throws NotEnoughQuantity {
		if (child.getWorkingQty() > getAvailableQty()) {
			throw new NotEnoughQuantity();
		}
		this.workingQtyOfChildren += child.getWorkingQty();
		this.children.add(child);
		if (child.parent != null) {
			child.parent.removeChild(child);
		}
		child.parent = this;
	}

	//TODO use this for force cancel
	public void removeChild(HierarchyOrder child) {
		this.children.remove(child);
		child.parent = null;
		addWorkingQtyChange(-child.getWorkingQty());
	}

	void addWorkingQtyChange(long qtyChange) {
		this.workingQtyOfChildren += qtyChange;
		if (order.cancelRequest.isPending() && workingQtyOfChildren <= 0) {
			order.cancel();
		} else if (replaceRequested > 0 && children.stream().allMatch(o->o.getWorkingQty() <= 0 || o.orderTime > replaceRequested)) { //TODO optimize?
			order.replaceRequest.accept();
			replaceRequested = 0;
		}
	}

	public long getAvailableQty() {
		return order.view.getLeavesQty() - workingQtyOfChildren;
	}

	public void fill(long qty, double px) {
		order.fill(qty, px);
	}

	public Collection getChildren() {
		return unmodifiableCollection(children);
	}

	public void addListener(OrderListener<F, ParentOrder<F>> listener) {
		order.addListener(new OrderToParentOrderListener(this, listener));
	}

	public void removeListener(OrderListener<F, ParentOrder<F>> listener) {
		for (OrderListener orderListener : order.listeners) {
			if (orderListener instanceof OrderToParentOrderListener) {
				if (((OrderToParentOrderListener)orderListener).parentListener == listener) {
					order.removeListener(orderListener);
				}
			}
		}
	}

	//callers should really check first, and then never need to catch this exception
	public static class NotEnoughQuantity extends RuntimeException {}
}
