package broke.fix;

import broke.fix.misc.ExecType;
import broke.fix.misc.IdGenerator;
import broke.fix.misc.OrderListener;
import broke.fix.misc.ParentalOrderListener;

import java.util.ArrayList;
import java.util.Collection;

import static broke.fix.misc.ExecType.New;
import static broke.fix.misc.ExecType.Restated;

import static java.util.Collections.addAll;
import static java.util.Collections.unmodifiableCollection;

/*
 * when an order could have child orders, its Order object should be wrapped by an instance of this class
 */
public class Parental<F> extends Member<F, Parental<F>> {
	public final Collection<OrderListener<F, Parental<F>>> listeners = new ArrayList<>();
	private final Collection<Member> children = new ArrayList<>();
	private final IdGenerator idgen;
	private Order<F> order;
	private long workingQtyOfChildren;
	private long replaceRequested;

	public Parental(IdGenerator idgen) {
		this.idgen = idgen;
	}

	public Parental<F> init(Order<F> order, OrderListener<F, Parental<F>>... listeners) {
		this.order = order;
		this.listeners.clear();
		if (listeners == null) {
			addAll(this.listeners, listeners);
		}
		order.listeners.add(new ParentalOrderListener(this));
		children.clear();
		return this;
	}

	@Override
	protected long getWorkingQty() {
		return order.getWorkingQty();
	}

	@Override
	protected void notifyParentOfWorkingQtyChange() {
		order.notifyParentOfWorkingQtyChange();
	}

	@Override
	public void requestCancel() {
		requestCancel(null);
	}
	
	public void requestCancel(CharSequence clOrdID) {
		if (order.view().isRoot() ^ (clOrdID == null)) {
			throw new RuntimeException("clOrdID should be passed requestCancel on root, and only on root");
		}
		this.order.requestCancel(clOrdID);
		for (Member child : children) {
			if (child.getWorkingQty() > 0) {
				child.requestCancel();
			}
		}
	}

	public void requestReplace(CharSequence clOrdID, long origOrdModTime, long newOrderQty, F newFields) {
		this.replaceRequested = idgen.getTime();
		this.order.replaceRequest.init(clOrdID, origOrdModTime, newOrderQty, newFields);
		for (Member child : children) {
			//TODO way to compare F to check their price or quantity or whatever? and instead update their orderTime to replaceRequested
			child.requestCancel();
		}
	}

	public void addChild(Member child) throws NotEnoughQuantity {
		if (child.getWorkingQty() > getAvailableQty()) { //callers should check this
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
	public void removeChild(Member child) {
		this.children.remove(child);
		child.parent = null;
		addWorkingQtyChange(-child.getWorkingQty());
	}

	protected void addWorkingQtyChange(long qtyChange) {
		this.workingQtyOfChildren += qtyChange;
		if (order.cancelRequest.isPending() && workingQtyOfChildren <= 0) {
			order.cancel();
		} else if (replaceRequested > 0 && children.stream().allMatch(o->o.getWorkingQty() <= 0 || o.orderTime > replaceRequested)) { //TODO optimize?
			order.replaceRequest.accept();
			replaceRequested = 0;
		}
	}

	public Order.View view() {
		return order.view();
	}

	public long getAvailableQty() {
		return order.view().getLeavesQty() - workingQtyOfChildren;
	}

	public void fill(long qty, double px) {
		order.fill(qty, px);
	}

	public Collection getChildren() {
		return unmodifiableCollection(children);
	}

	public static class NotEnoughQuantity extends RuntimeException {}
}
