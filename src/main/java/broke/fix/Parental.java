package broke.fix;

import broke.fix.misc.ExecType;
import broke.fix.misc.FixFields;
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
public class Parental<F extends FixFields> extends Member<F, Parental<F>> {
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
		if (listeners != null) {
			addAll(this.listeners, listeners);
		}
		order.listeners.add(new ParentalOrderListener(this)); //TODO garbage
		children.clear();
		return this;
	}

	public void requestCancel(CharSequence clOrdID) {
		order.cancelRequest.init(clOrdID);
		for (Member child : children) {
			if (child.getWorkingQty() > 0) {
				child.requestCancel();
			}
		}
	}

	public void requestReplace(CharSequence clOrdID, F newFields) {
		replaceRequested = incoming.getTime();
		order.replaceRequest.init(clOrdID, newFields);
		for (Member child : children) {
			child.requestCancel();
			//TODO can we determine that it's a just a price amend,
			// and instead of cancelling the children that are already priced properly, update their orderTime?
			//
			//TODO can we determine that it's just a quantity amend down,
			// and instead of cancelling some of the children that sum to less than or equal to the new quantity, update their orderTime?
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
		} else if (replaceRequested > 0 && children.stream().allMatch(o->o.getWorkingQty() <= 0 || o.view().getOrderTime() > replaceRequested)) { //TODO optimize?
			order.replaceRequest.accept();
			replaceRequested = 0;
		}
	}

	protected void fill(long qty, double px) {
		order.fill(qty, px);
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
