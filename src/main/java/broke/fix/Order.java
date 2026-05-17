package broke.fix;

import static broke.fix.dto.ExecInst.Suspend;
import static java.lang.Long.max;

import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import broke.fix.dto.ExecType;
import broke.fix.dto.OrdStatus;
import broke.fix.misc.FixFields;
import broke.fix.misc.IncomingContext;
import broke.fix.misc.NotEnoughQtyException;
import broke.fix.misc.OrderCategory;
import broke.fix.misc.OrderListener;

public abstract class Order<F extends FixFields> {
	private final static Logger log = LogManager.getLogger();
	private final IncomingContext context;
	private final Collection<OrderListener<Order<F>, F>> listeners;
	private final Deque<Request> pendingRequsts = new LinkedList<>();
	private final OrderCategory category;
	private CharSequence clOrdID;
	private CompositeOrder<F> parent;
	private F fields;
	private long cumQty, transactTime, internalOrderID;
	private double avgPx;
	private OrdStatus terminalOrdStatus;
	private long requestCounter;

	//cannot be negative
	public abstract long getWorkingQty();

	public Order(IncomingContext context, F fields, Collection<OrderListener<Order<F>, F>> listeners) {
		this.context = context;
		this.fields = fields;
		this.listeners = listeners;
		this.internalOrderID = context.generateOrderID();
		this.transactTime = context.transactTime;
		this.category = getCategory();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected static <F extends FixFields> Collection<OrderListener<Order<F>, F>> safeAsList(OrderListener<? extends Order<F>, F>[] a) {
		return (Collection)Arrays.asList(a);
	}

	//fill() and addWorkingQtyChange() are the contract between an Order and its parent

	protected void fill(OrderCategory category, final long qty, final double px) {
		double totalValue = qty * px + cumQty * avgPx;
		avgPx = totalValue / (qty + cumQty);
		cumQty += qty;
		if (parent != null) {
			parent.fill(category, qty, px);
		}
		endTransaction(l->l.onTrade(this, ExecType.Trade, qty, px));
	}

	//not called from fill() here, because called from fill() in CompositeOrder
	protected void addWorkingQtyChange(OrderCategory category, long qtyChange) {
		if (parent != null) {
			parent.addWorkingQtyChange(category, qtyChange);
		}
	}

	//package private to encourage instead calling either forceCancel() or new CancelRequest()
	void cancel(CharSequence text) {
		terminate(OrdStatus.Canceled, ExecType.Canceled, text);
	}

	public void forceCancel(CharSequence text) {
		cancel(text);
	}

	public final void replace(F fields) throws NotEnoughQtyException {
		if (!canReplace(fields)) { //callers should check this
			throw new NotEnoughQtyException();
		}
		this.fields = fields;
		endTransaction(l->l.onOtherExecutionReport(this, ExecType.Replaced, null, null));
	}

	//meaning: can the replace be accepted right now without first taking any other action?
	public boolean canReplace(F requestedFields) {
		if (requestedFields.getOrigOrdModTime() > 0 && requestedFields.getOrigOrdModTime() != getTransactTime()) {
			return false;
		}
		return parent == null || requestedFields.getOrderQty() - this.fields.getOrderQty() <= parent.getAvailableQty();
	}
	
	public void terminate(OrdStatus status, ExecType execType, CharSequence reason) {
		terminalOrdStatus = status;
		addWorkingQtyChange(category, -getWorkingQty());
		endTransaction(l->l.onOtherExecutionReport(this, execType, null, reason));
	}

	//onRequestChange() and endTransaction() are the generic contract that Order provides to Request (in addition to cancel() and replace())

	void onRequestChange(Request request, Request.Status requestStatus) {
		if (requestStatus == Request.Status.Pending) {
			pendingRequsts.add(request);
		} else {
			pendingRequsts.remove(request);
		}
	}

	protected final void endTransaction(Consumer<OrderListener<Order<F>,F>> listenerCall) {
		transactTime = context.transactTime;
		for (OrderListener<Order<F>, F> listener : listeners) {
			try {
				listenerCall.accept(listener);
			} catch (RuntimeException e) {
				log.warn("Listener exception", e);
			}
		}
	}

	public final Iterable<Request> pendingRequsts() {
		return pendingRequsts;
	}

	public final OrdStatus getOrdStatus() {
		return
			!pendingRequsts.isEmpty()      ? pendingRequsts.getLast().getPendingOrdStatus() :
			terminalOrdStatus != null      ? terminalOrdStatus :
			fields.hasExecInst(Suspend)    ? OrdStatus.Suspended :
			isFullyFilled()                ? OrdStatus.Filled :
			cumQty > 0                     ? OrdStatus.PartiallyFilled :
											 OrdStatus.New;
	}

	public final F getFields() {
		return fields;
	}

	public final long getCumQty() {
		return cumQty;
	}

	//could be negative if order was overfilled
	public final long getLeavesQty() {
		return terminalOrdStatus != null ? 0 : max(0, fields.getOrderQty() - cumQty);
	}

	public OrderCategory getCategory() {
		return OrderCategory.Other;
	}

	public final double getAvgPx() {
		return avgPx;
	}

	public final long getInternalOrderID() {
		return internalOrderID;
	}

	public final long getTransactTime() {
		return transactTime;
	}

	public final boolean isRoot() {
		return parent == null;
	}

	public final CompositeOrder<F> getParent() {
		return parent;
	}

	protected final void setParent(CompositeOrder<F> parent) {
		this.parent = parent;
	}

	public final boolean isFullyFilled() {
		return cumQty > 0 && cumQty >= fields.getOrderQty();
	}

	public final boolean isClosed() {
		return terminalOrdStatus != null || isFullyFilled();
	}

	public final CharSequence getClOrdID() {
		return clOrdID;
	}

	protected final void setClOrdID(CharSequence clOrdID) {
		this.clOrdID = clOrdID;
	}

	public final CharSequence nextClOrdID() {
		return clOrdID = getInternalOrderID()+"."+requestCounter++;
	}
}
