package broke.fix;

import static broke.fix.misc.Util.execRestatementReason;
import static broke.fix.misc.Util.ordRejReason;
import static broke.fix.dto.ExecInst.Suspend;
import static java.lang.Long.max;
import static java.lang.System.nanoTime;

import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import broke.fix.dto.CxlRejReason;
import broke.fix.dto.ExecType;
import broke.fix.dto.OrdStatus;
import broke.fix.misc.FixFields;
import broke.fix.misc.IncomingContext;
import broke.fix.misc.NotEnoughQtyException;
import broke.fix.misc.OrderListener;
import broke.fix.request.CancelRequest;
import broke.fix.request.NewRequest;
import broke.fix.request.ReplaceRequest;

public abstract class Order<F extends FixFields> {
	private final static Logger log = LogManager.getLogger();
	private final IncomingContext context;
	protected final Deque<Request<F>> requests = new LinkedList<>();
	protected final Collection<OrderListener> listeners;
	protected CharSequence clOrdID;
	private CompositeOrder<F> parent;
	private F fields;
	private long cumQty, transactTime, internalOrderID;
	private double avgPx;
	private OrdStatus terminalOrdStatus;

	public Order(IncomingContext context, F fields, Collection<OrderListener> listeners) {
		this.context = context;
		this.fields = fields;
		this.listeners = listeners;
		this.internalOrderID = nanoTime();
		this.transactTime = context.transactTime;
		endTransaction(l->l.onNewRequest(this));
	}

	public final void fill(final long qty, final double px) {
		double totalValue = qty * px + cumQty * avgPx;
		avgPx = totalValue / (qty + cumQty);
		cumQty += qty;
		if (parent != null) {
			parent.fill(qty, px);
		}
		endTransaction(l->l.onTrade(this, ExecType.Trade, qty, px));

		if (isFullyFilled()) {
			for (Request<F> request : requests) {
				request.onFill();
			}
		}
	}

	 void cancel() {
		terminate(OrdStatus.Canceled, ExecType.Canceled, null); //TODO param for reason?
	}

	protected void replace(F fields) throws NotEnoughQtyException {
		if (!canReplace(fields)) { //callers should check this
			throw new NotEnoughQtyException();
		}
		this.fields = fields;
		endTransaction(l->l.onOtherExecutionReport(this, ExecType.Replaced, null, null));
	}

	public boolean canReplace(F newFields) {
		if (newFields.getOrigOrdModTime() > 0 && newFields.getOrigOrdModTime() != getTransactTime()) {
			return false;
		}
		return parent == null || fields.getOrderQty() - this.fields.getOrderQty() <= parent.getAvailableQty();
	}
	
	protected final void terminate(final OrdStatus status, final ExecType execType, Object reason) {
		//TODO maybe respond with the clordid of a cancel request, or a replace down
		terminalOrdStatus = status;
		addWorkingQtyChange(-getWorkingQty());
		endTransaction(l->l.onOtherExecutionReport(this, execType, ordRejReason(reason), execRestatementReason(reason)));

		for (Request<F> request : requests) {
			if (request instanceof ReplaceRequest) {
				request.reject(CxlRejReason.TooLateToCancel);
			}
		}
	}

	protected final void endTransaction(Consumer<OrderListener<Order<F>>> listenerCall) {
		transactTime = context.transactTime;
		for (OrderListener<Order<F>> listener : listeners) {
			try {
				listenerCall.accept(listener);
			} catch (RuntimeException e) {
				log.warn("Listener exception", e);
			}
		}
	}

	public final OrdStatus getOrdStatus() {
		return
			terminalOrdStatus != null      ? terminalOrdStatus :
			!requests.isEmpty()            ? requests.getLast().getPendingStatus() :
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

	public final long getLeavesQty() {
		return terminalOrdStatus != null ? 0 : max(0, fields.getOrderQty() - cumQty);
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

	private final boolean isFullyFilled() { //not public because could still have pending requests, which might surprise people
		return cumQty > 0 && cumQty >= fields.getOrderQty();
	}

	public final boolean isWorking() {
		return terminalOrdStatus == null;
	}

	protected void addWorkingQtyChange(long qtyChange) {
		if (parent != null) {
			parent.addWorkingQtyChange(qtyChange);
		}
	}

	protected void onAccept(Request<F> request) {
	}

	protected void onReject(Request<F> request) {
	}

	public abstract long getWorkingQty();
	public abstract void forceCancel();
	public abstract NewRequest<F> requestNew();
	public abstract CancelRequest<F> requestCancel();
	public abstract ReplaceRequest<F> requestReplace(F fields);
}
