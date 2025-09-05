package broke.fix;

import broke.fix.misc.CxlRejReason;
import broke.fix.misc.ExecType;
import broke.fix.misc.IdGenerator;
import broke.fix.misc.OrderListener;
import broke.fix.misc.OrdStatus;
import broke.fix.Parental.NotEnoughQuantity;
import broke.fix.CancelRequest;
import broke.fix.NewRequest;
import broke.fix.ReplaceRequest;
import broke.fix.Request;
import java.util.Collection;
import java.util.ArrayList;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static java.util.Collections.addAll;
import static java.lang.Long.max;
import static broke.fix.misc.ExecType.Canceled;
import static broke.fix.misc.ExecType.DoneForDay;
import static broke.fix.misc.ExecType.New;
import static broke.fix.misc.ExecType.Replaced;
import static broke.fix.misc.ExecType.Restated;

public class Order<F> extends Member<F, Order<F>> {
	private F fields;
	private long orderQty, leavesQty, cumQty, origWorkingQty, orderID;
	private double avgPx;
	private OrdStatus terminalOrdStatus;
	private long transactTime; //the transactTime of the New and updated on each ExecutionReport
	private final IdGenerator idgen;
	private final View view = new View();
	public final Collection<OrderListener<F, Order<F>>> listeners = new ArrayList<>();
	public final NewRequest<F> newRequest = new NewRequest(this);
	public final ReplaceRequest<F> replaceRequest = new ReplaceRequest(this);
	public final CancelRequest cancelRequest = new CancelRequest(this);

	public Order(IdGenerator idgen) {
		this.idgen = idgen;
	}

	public Order<F> init(CharSequence clOrdID, long orderQty, F fields, OrderListener<F, Order<F>>... listeners) {
		newRequest.init(clOrdID);
		replaceRequest.reset();
		cancelRequest.reset();
		this.fields = fields;
		this.leavesQty = this.orderQty = orderQty;
		avgPx = cumQty = 0;
		this.listeners.clear();
		this.listeners.add(new WorkingQtyListener<>());
		if (listeners != null) {
			addAll(this.listeners, listeners);
		}
		orderTime = idgen.getTime();
		orderID = idgen.getOrderID();
		transactTime = idgen.incomingTransactTime;
		end(l->l.onNewRequest(this));
		return this;
	}

	public void fill(final long qty, final double px) {
		begin();
		double totalValue = qty * px + cumQty * avgPx;
		avgPx = totalValue / (qty + cumQty);
		cumQty += qty;
		leavesQty -= qty;
		if (!isWorking()) {
			terminalOrdStatus = OrdStatus.Filled;
		}
		if (parent != null) {
			parent.fill(qty, px);
		}
		transactTime = idgen.incomingTransactTime;
		end(l->l.onFill(this, qty, px));

		//separate transaction
		if (replaceRequest.isPending() && leavesQty <= replaceRequest.getQty() && replaceRequest.getQty() != orderQty) {
			replaceRequest.accept(); // https://www.onixs.biz/fix-dictionary/4.2/app_d12.html
		} else if (!isWorking()) {
			if (cancelRequest.isPending()) {
				cancelRequest.reject();
			}
			if (replaceRequest.isPending()) {
				replaceRequest.reject();
			}
		}
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
		requestCancel(null);
	}
	
	//clOrdID is only passed in on the root order
	public void requestCancel(CharSequence clOrdID) {
		begin();
		cancelRequest.init(clOrdID != null ? clOrdID : idgen.getClOrdID());
		end(l->l.onCancelRequest(this));
	}

	public void replace(F fields, long orderQtyChange) throws NotEnoughQuantity {
		begin();
		if (fields == null) {
			if (parent != null && orderQtyChange > parent.getAvailableQty()) { //callers should check this
				throw new NotEnoughQuantity();
			}
		} else {
			this.fields = fields;
		}
		leavesQty += orderQtyChange;
		if (isWorking()) {
			orderTime = idgen.getTime();
		} else {
			terminalOrdStatus = OrdStatus.Filled;
		}
		transactTime = idgen.incomingTransactTime;
		end(l->l.onNonFillExecutionReport(this, fields == null ? Restated : Replaced));

		//separate transaction
		if (!isWorking()) {
			if (cancelRequest.isPending()) {
				cancelRequest.reject();
			}
		}
	}

	public void cancel() {
		terminate(OrdStatus.Canceled, ExecType.Canceled);
	}

	public void done() {
		terminate(OrdStatus.DoneForDay, ExecType.DoneForDay);
	}

	protected void terminate(final OrdStatus status, final ExecType execType) {
		begin();
		cancelRequest.reset();
		terminalOrdStatus = status;
		leavesQty = 0;
		transactTime = idgen.incomingTransactTime;
		end(l->l.onNonFillExecutionReport(this, execType));

		//separate transaction
		if (cancelRequest.isPending()) {
			cancelRequest.reject();
		}
		if (replaceRequest.isPending()) {
			replaceRequest.reject();
		}
	}

	protected void begin() {
		origWorkingQty = getWorkingQty();
	}

	protected void end(Consumer<OrderListener> listenerCall) {
		listeners.forEach(listenerCall);
	}

	protected void notifyParentOfWorkingQtyChange() {
		//all methods which invoke OrderListener must do so at the end, and call begin() at the beginning
		if (parent != null) {
			parent.addWorkingQtyChange(getWorkingQty() - origWorkingQty); //can be negative
		}
	}

	public boolean isWorking() {
		return leavesQty > 0;
	}

	public View view() {
		return view;
	}
	
	public class View {
		public OrdStatus getOrdStatus() {
			return
				cancelRequest.isPending()  ? cancelRequest.getStatus() :
				replaceRequest.isPending() ? replaceRequest.getStatus() :
				terminalOrdStatus != null  ? terminalOrdStatus :
				cumQty > 0                 ? OrdStatus.PartiallyFilled :
			                                     newRequest.getStatus();
		}

		public long getOrderQty() {
			return orderQty;
		}

		public boolean isPending() {
			return newRequest.isPending();
		}

		public boolean isRoot() {
			return parent == null;
		}

		public F getFields() {
			return fields;
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

		public long getOrderID() {
			return orderID;
		}

		public CharSequence getDownstreamOrderID() {
			return newRequest.getDownstreamOrderID();
		}

		public CharSequence getClOrdID() {
			return newRequest.getClOrdID();
		}

		public CharSequence getOrigClOrdID() {
			return newRequest.getOrigClOrdID();
		}

		public long getTransactTime() {
			return transactTime;
		}

		public Parental getParent() {
			return parent;
		}
	}
}
