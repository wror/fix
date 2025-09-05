package broke.fix;

import broke.fix.misc.ExecType;
import broke.fix.misc.IncomingContext;
import broke.fix.misc.IdGenerator;
import broke.fix.misc.NotEnoughQtyException ;
import broke.fix.misc.OrderListener;
import broke.fix.misc.ParentalOrderListener;

import java.util.ArrayList;
import java.util.Collection;

import static broke.fix.misc.ExecType.New;
import static broke.fix.misc.ExecType.Restated;

import static java.util.Collections.addAll;
import static java.util.Collections.unmodifiableCollection;

/*
 * When an order could have child orders, its Order object should be wrapped by an instance of this class.
 *
 * It's called a "parental" to distinguish from the word "parent" which describes a logical relationship.
 */
public class Parental<F> extends Member<F, Parental<F>> {
	public final Collection<OrderListener<F, Parental<F>>> listeners = new ArrayList<>();
	private final Collection<Member> children = new ArrayList<>();
	private final IncomingContext incoming;
	private Order<F> order;
	private long workingQtyOfChildren;
	private long replaceRequested;

	public Parental(IncomingContext incoming) {
		this.incoming = incoming;
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
		this.replaceRequested = incoming.getTime();
		this.order.replaceRequest.init(clOrdID, origOrdModTime, newOrderQty, newFields);
		for (Member child : children) {
			//TODO way to compare F to check their price or quantity or whatever? and instead update their orderTime to replaceRequested
			child.requestCancel();
		}
	}

	public void addChild(Member child) throws NotEnoughQtyException {
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

	public void removeChild(Member child) {
		child.setParent(null);
		children.remove(child);
		addWorkingQtyChange(-child.getWorkingQty());
	}

	protected void addWorkingQtyChange(long qtyChange) {
		this.workingQtyOfChildren += qtyChange;
		if (order.cancelRequest.isPending() && workingQtyOfChildren <= 0) {
			order.cancel();
		} else if (replaceRequested > 0 && children.stream().allMatch(o->o.getWorkingQty() <= 0 || o.view().getOrderTime() > replaceRequested)) { //TODO optimize?
			order.replaceRequest.accept();
			replaceRequested = 0;
		}
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

	@Override
	protected void setParent(Parental parent) {
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
	public Collection<OrderListener<F, Parental<F>>> listeners() {
		return listeners;
	}

	@Override
	public Order.View view() {
		return order.view();
	}
}
