package broke.fix;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import broke.fix.misc.FixFields;
import broke.fix.misc.IncomingContext;
import broke.fix.misc.NotEnoughQtyException;
import broke.fix.misc.OrderListener;
import broke.fix.request.CancelRequest;
import broke.fix.request.NewRequest;
import broke.fix.request.ReplaceRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static java.util.Collections.unmodifiableCollection;

public final class CompositeOrder<F extends FixFields> extends Order<F> {
	private final static Logger log = LogManager.getLogger();
	private final Collection<Order<F>> children = new ArrayList<>();
	private long workingQtyOfChildren;
	private Runnable onNoWorkingQty;

	public CompositeOrder(IncomingContext context, F fields, OrderListener<CompositeOrder<F>>... listeners) {
		super(context, fields, new ArrayList<>(Arrays.asList(listeners)));
	}

	@Override
	protected void onAccept(Request<F> request) {
		clOrdID = request.getClOrdID(); // pessimistic per https://www.onixs.biz/fix-dictionary/4.4/msgType_G_71.html
	}

	@Override
	public NewRequest<F> requestNew() {
		return new NewRequest<>(null, this);
	}

	@Override
	public ReplaceRequest<F> requestReplace(F newFields) {
		if (onNoWorkingQty != null) {
			log.warn("Replacing an order hierarchy before a previous replace completed!");
		}
		return cancelFor(new ReplaceRequest<>(clOrdID, null, this, newFields));
	}

	@Override
	public CancelRequest<F> requestCancel() {
		return cancelFor(new CancelRequest<>(clOrdID, null, this));
	}

	private <R extends Request<F>> R cancelFor(R r) {
		onNoWorkingQty = ()->r.accept();
		for (Order<F> child : children) {
			if (child.getWorkingQty() > 0) {
				child.requestCancel();
			}
		}
		return r;
	}

	@Override
	public void forceCancel() {
		for (Order<F> child : children) {
			child.forceCancel();
		}
		cancel();
	}

	@Override
	public boolean canReplace(F fields) {
		if (!super.canReplace(fields)) {
			return false;
		}
		if (fields.getOrderQty() < workingQtyOfChildren) {
			return false;
		}
		if (fields.isPriceLessGenerousThan(this.getFields().getPrice())) {
			for (Order<F> child : children) {
				if (fields.isPriceLessGenerousThan(child.getFields().getPrice()) && child.isWorking()) {
					return false;
				}
			}
		}
		//TODO other fields?
		return true;
	}

	public void addChild(Order<F> child) throws NotEnoughQtyException {
		if (child.getWorkingQty() > getAvailableQty()) { //callers should check this
			throw new NotEnoughQtyException();
		}
		if (child.getParent() != null) {
			child.getParent().removeChild(this);
		}
		child.setParent(this);
		children.add(child);
		addWorkingQtyChange(child.getWorkingQty());
	}

	public void removeChild(Order<F> child) {
		child.setParent(null);
		children.remove(child);
		addWorkingQtyChange(-child.getWorkingQty());
	}

	public long getAvailableQty() {
		return getLeavesQty() - workingQtyOfChildren;
	}

	public Collection<Order<F>> getChildren() {
		return unmodifiableCollection(children);
	}

	@Override
	protected void addWorkingQtyChange(long qtyChange) {
		this.workingQtyOfChildren += qtyChange;
		super.addWorkingQtyChange(qtyChange);

		if (onNoWorkingQty != null && workingQtyOfChildren == 0) {
			onNoWorkingQty.run();
			onNoWorkingQty = null;
		}
	}

	@Override
	public long getWorkingQty() {
		return workingQtyOfChildren;
	}
}
