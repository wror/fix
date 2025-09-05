package m15xb7.fix;

import m15xb7.fix.misc.ExecType;
import m15xb7.fix.misc.IdGenerator;
import m15xb7.fix.misc.OrderListener;
import m15xb7.fix.misc.OrdStatus;
import m15xb7.fix.ParentOrder.NotEnoughQuantity;
import java.util.Collection;
import java.util.ArrayList;

import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static java.lang.Long.max;
import static m15xb7.fix.misc.ExecType.Canceled;
import static m15xb7.fix.misc.ExecType.DoneForDay;
import static m15xb7.fix.misc.ExecType.Replaced;
import static m15xb7.fix.misc.ExecType.Restated;

public class Order<F, R> extends HierarchyOrder<F, Order<F, R>> {
	private F fields;
	private long orderQty, leavesQty, cumQty, origWorkingQty;
	private double avgPx;
	private OrdStatus terminalOrdStatus;
	private long transactTime; //the transactTime of the New and updated on each ExecutionReport
	CharSequence orderID, clOrdID;

	Collection<OrderListener<F, Order<F, R>>> listeners;
	IdGenerator idgen;
	public final RequestNew newRequest = new RequestNew(this);
	public final RequestReplace replaceRequest = new RequestReplace(this);
	public final Request cancelRequest = new RequestCancel(this);

	public Order<F, R> init(IdGenerator idgen, ParentOrder<F> parent, F fields, long orderQty) {
		this.idgen = idgen;
		this.parent = parent;
		this.fields = fields;
		this.orderQty = orderQty;
		this.leavesQty = orderQty;
		this.orderTime = idgen.getTime();
		this.listeners = new ArrayList<>();
		this.transactTime = idgen.incomingTransactTime;
		addListener(new WorkingQtyListener<>());
		return this;
	}

	public void addListener(OrderListener<F, Order<F, R>> listener) {
		this.listeners.add(listener);
		listener.onNew(this);
	}

	public void removeListener(OrderListener<F, Order<F, R>> listener) {
		this.listeners.remove(listener);
	}

	public void fill(long qty, double px) {
		begin();
		double totalValue = qty * px + cumQty * avgPx;
		avgPx = totalValue / (qty + cumQty);
		cumQty += qty;
		leavesQty -= qty;
		if (!isWorking()) {
			if (replaceRequest.isPending() && replaceRequest.getQty() != orderQty) {
				replaceRequest.status = Request.Status.Accepted;
			}
			this.terminalOrdStatus = OrdStatus.Filled;
		}
		if (parent != null) {
			parent.fill(qty, px);
		}
		endAccepted(l->l.onFill(this, qty, px));
	}

	public boolean isWorking() {
		return leavesQty > 0;
	}
	
	@Override
	public long getWorkingQty() {
		if (!isWorking()) {
			return 0;
		}
		long potentialOrderQty = max(replaceRequest.isPending() ? replaceRequest.getQty() : 0, orderQty);
		return max(0, potentialOrderQty - cumQty);
	}

	@Override
	public void requestCancel() {
		begin();
		cancelRequest.init(idgen.getClOrdID());
		end(l->l.onCancelRequest(this));
	}

	public void requestReplace(F newFields, long newOrderQty) throws NotEnoughQuantity {
		begin();
		if (parent != null && replaceRequest.getQty() - orderQty > parent.getAvailableQty()) {
			throw new NotEnoughQuantity();
		}
		replaceRequest.init(idgen.getClOrdID(), newFields, newOrderQty);
		end(l->l.onReplaceRequest(this));
	}

	public void replace(F newFields, long requestedOrderQty) {
		begin();
		long newOrderQty = Math.max(requestedOrderQty, cumQty);
		leavesQty += newOrderQty - orderQty;
		orderQty = newOrderQty;
		if (isWorking()) {
			this.orderTime = idgen.getTime();
			fields = newFields;
		} else {
			this.terminalOrdStatus = OrdStatus.Filled;
		}
		endAccepted(l->l.onNonFillExecutionReport(this, Replaced));
	}

	public void cancel() {
		cancel(leavesQty);
	}

	public void cancel(long qtyChange) {
		begin();
		leavesQty -= qtyChange;
		if (!isWorking()) {
			terminate(OrdStatus.Canceled);
		}
		endAccepted(l->l.onNonFillExecutionReport(this, isWorking() ? Restated : Canceled));
	}

	public void done() {
		begin();
		terminate(OrdStatus.DoneForDay);
		endAccepted(l->l.onNonFillExecutionReport(this, DoneForDay));
	}

	void terminate(OrdStatus status) {
		cancelRequest.status = Request.Status.Accepted;
		terminalOrdStatus = status;
		leavesQty = 0;
	}

	void begin() {
		this.origWorkingQty = getWorkingQty();
	}

	void end(Consumer<OrderListener> listenerCall) {
		listeners.forEach(listenerCall);
	}

	private void endAccepted(Consumer<OrderListener> listenerCall) {
		this.transactTime = idgen.incomingTransactTime;
		end(listenerCall);
//TODO CxlRejReason = 0 https://www.onixs.biz/fix-dictionary/4.4/tagNum_102.html
		if (!isWorking()) {
			if (cancelRequest.isPending()) {
				cancelRequest.reject();
			}
			if (replaceRequest.isPending()) {
				replaceRequest.reject();
			}
		}
	}

	void notifyParentOfWorkingQtyChange() {
		//all methods which invoke OrderListener must do so at the end, and call begin() at the beginning
		if (parent != null) {
			parent.addWorkingQtyChange(getWorkingQty() - origWorkingQty); //can be negative
		}
	}

	public final View view = new View();

	public class View {
		public boolean isWorking() {
			return isWorking();
		}

		public OrdStatus getOrdStatus() {
			return
				cancelRequest.isPending()  ? cancelRequest.getStatus() :
				replaceRequest.isPending() ? replaceRequest.getStatus() :
				terminalOrdStatus != null  ? terminalOrdStatus :
				cumQty > 0                 ? OrdStatus.PartiallyFilled :
			                             	newRequest.getStatus();
		}

		public R getRootFields() {
			if (parent == null) {
				return (R)fields;
			}
			return (R)parent.order.view.getRootFields();
		}

		public F getFields() {
			return fields;
		}

		public long getOrderQty() {
			return orderQty;
		}

		public long getCumQty() {
			return cumQty;
		}

		public long getLeavesQty() {
			return leavesQty;
		}

		public double getAvgPx() {
			return avgPx;
		}

		public CharSequence getClOrdID() {
			return clOrdID;
		}

		public long getTransactTime() {
			return transactTime;
		}
	}
}
