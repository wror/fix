package broke.fix;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import broke.fix.misc.FixFields;
import broke.fix.misc.IncomingContext;
import broke.fix.misc.NotEnoughQtyException;
import broke.fix.misc.OrderListener;
import broke.fix.request.CancelRequest;

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
	public void onRequestChange(Request<?, F> request) {
		super.onRequestChange(request);
		if (request.getStatus() == Request.Status.Pending && request.getQty() < getFields().getOrderQty()) {
			if (onNoWorkingQty != null) {
				log.warn("Two order hierarchy requests working at the same time!");
			}
			cancelChildrenFor(request);
		} else if (request.getStatus() == Request.Status.Accepted) {
			setClOrdID(request.getClOrdID()); // pessimistic per https://www.onixs.biz/fix-dictionary/4.4/msgType_G_71.html
		}
	}

	private <R extends Request<?, F>> void cancelChildrenFor(R r) {
		onNoWorkingQty = ()->r.accept();
		for (Order<F> child : children) {
			if (child.getWorkingQty() > 0) {
				new CancelRequest(child);
			}
		}
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
}
