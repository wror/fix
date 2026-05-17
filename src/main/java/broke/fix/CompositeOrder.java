package broke.fix;

import broke.fix.dto.ExecType;
import broke.fix.dto.OrdStatus;
import broke.fix.misc.FixException.Reason;
import broke.fix.misc.FixFields;
import broke.fix.misc.IncomingContext;
import broke.fix.misc.NotEnoughQtyException;
import broke.fix.misc.OrderCategory;
import broke.fix.misc.OrderListener;
import broke.fix.request.CancelRequest;
import broke.fix.upstream.UpstreamRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

import static java.util.Collections.unmodifiableCollection;

//To be filled by child orders. Can represent an order directly from upstream, or can itself have a parent.
public final class CompositeOrder<F extends FixFields> extends Order<F> {
	private final Collection<Order<F>> children = new ArrayList<>();
	private final UpstreamRepository<F> repo;
	private long workingQtyOfChildren;
	private Runnable onNoWorkingQty;
	private Map<OrderCategory, Double> workingQtys = new EnumMap<>(OrderCategory.class), cumQtys = new EnumMap<>(OrderCategory.class);

	@SafeVarargs
	public CompositeOrder(IncomingContext context, F fields, UpstreamRepository<F> repo, OrderListener<CompositeOrder<F>, F>... listeners) {
		super(context, fields, safeAsList(listeners));
		this.repo = repo;
	}

	@Override
	void onRequestChange(Request request, Request.Status requestStatus) {
		super.onRequestChange(request, requestStatus);
		if (requestStatus == Request.Status.Pending && request.getRequestedOrderQty() < getFields().getOrderQty()) {
			cancelChildrenFor(request);
		}
		if (requestStatus == Request.Status.Accepted) {
			setClOrdID(request.clOrdID); // pessimistic per https://www.fixtrading.org/online-specification/business-area-trade/#:~:text=The%20order%20sender%20should%20chain,this.%29
		}
		if (requestStatus != Request.Status.Pending && repo != null) {
			repo.remove(request);
		}
	}

	private <R extends Request> void cancelChildrenFor(R r) {
		onNoWorkingQty = () -> {
			r.accept();
			onNoWorkingQty = null;
		};
		for (Order<F> child : children) {
			if (child.getWorkingQty() > 0) {
				new CancelRequest(child);
			}
		}
	}

	@Override
	public void forceCancel(CharSequence text) {
		for (Order<F> child : children) {
			child.forceCancel(text);
		}
		cancel(text);
	}

	@Override
	public boolean canReplace(F requestedFields) {
		if (!super.canReplace(requestedFields)) {
			return false;
		}
		if (requestedFields.getOrderQty() < workingQtyOfChildren) {
			return false;
		}
		if (requestedFields.areMoreRestrictiveThan(getFields())) {
			for (Order<F> child : children) {
				if (requestedFields.areMoreRestrictiveThan(child.getFields()) && !child.isClosed()) {
					return false;
				}
			}
		}
		return true;
	}
	
	@Override
	public void terminate(final OrdStatus status, final ExecType execType, CharSequence reason) {
		super.terminate(status, execType, reason);

		for (Request request : pendingRequsts()) {
			if (request.getRequestedOrderQty() >= getFields().getOrderQty()) {
				request.reject(Reason.TooLate);
			} else {
				setClOrdID(request.clOrdID);
			}
		}
		if (repo != null) {
			repo.remove(this);
		}
	}

	@Override
	protected void fill(OrderCategory category, long fillQty, double px) {
		super.fill(category, fillQty, px);
		add(cumQtys, category, fillQty);
		addWorkingQtyChangeOnThis(category, -fillQty);

		for (Request request : pendingRequsts()) {
			request.onFill();
		}
		if (repo != null && isClosed()) {
			repo.remove(this);
		}
	}

	@Override
	protected void addWorkingQtyChange(OrderCategory category, long qtyChange) {
		super.addWorkingQtyChange(category, qtyChange);
		addWorkingQtyChangeOnThis(category, qtyChange);
	}

	private void addWorkingQtyChangeOnThis(OrderCategory category, long qtyChange) {
		this.workingQtyOfChildren += qtyChange;
		add(workingQtys, category, qtyChange);
		if (onNoWorkingQty != null && workingQtyOfChildren == 0) {
			onNoWorkingQty.run();
			onNoWorkingQty = null;
		}
	}

	private void add(Map<OrderCategory, Double> qtys, OrderCategory category, long qtyChange) {
		qtys.put(category, qtys.getOrDefault(category, 0.)+qtyChange);
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
		addWorkingQtyChange(child.getCategory(), child.getWorkingQty());
	}

	public void removeChild(Order<F> child) {
		child.setParent(null);
		children.remove(child);
		addWorkingQtyChange(child.getCategory(), -child.getWorkingQty());
	}

	public long getAvailableQty() {
		return getLeavesQty() - workingQtyOfChildren;
	}

	public Collection<Order<F>> getChildren() {
		return unmodifiableCollection(children);
	}

	public double getWorkingQtyFor(OrderCategory category) {
		return workingQtys.get(category);
	}

	public double getCumQtyFor(OrderCategory category) {
		return cumQtys.get(category);
	}
}
