package broke.fix;

import broke.fix.misc.CxlRejReason;
import broke.fix.misc.ExecType;
import broke.fix.misc.FixFields;
import broke.fix.misc.IncomingContext;
import broke.fix.misc.IdGenerator;
import broke.fix.misc.OrderListener;
import broke.fix.misc.OrdStatus;
import broke.fix.misc.NotEnoughQtyException;
import broke.fix.CancelRequest;
import broke.fix.NewRequest;
import broke.fix.Parental;
import broke.fix.ReplaceRequest;
import broke.fix.Request;
import java.util.Collection;
import java.util.ArrayList;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static java.util.Collections.addAll;
import static java.lang.Long.max;
import static broke.fix.misc.ExecInst.Suspend;
import static broke.fix.misc.ExecType.Canceled;
import static broke.fix.misc.ExecType.DoneForDay;
import static broke.fix.misc.ExecType.New;
import static broke.fix.misc.ExecType.Replaced;
import static broke.fix.misc.ExecType.Restated;

public class Order<F extends FixFields> extends Member<F, Order<F>> {
	private Parental parent;
	private F fields;
	private long cumQty, origWorkingQty, orderID, transactTime, orderTime;
	private double avgPx;
	private OrdStatus terminalOrdStatus;
	private final IncomingContext incoming;
	private final View view = new View();
	public final IdGenerator idgen;
	public final Collection<OrderListener<F, Order<F>>> listeners = new ArrayList<>();
	public final NewRequest<F> newRequest = new NewRequest(this);
	public final ReplaceRequest<F> replaceRequest = new ReplaceRequest(this);
	public final CancelRequest cancelRequest = new CancelRequest(this);

	public Order(IncomingContext incoming, IdGenerator idgen) {
		this.incoming = incoming;
		this.idgen = idgen;
	}

	public Order<F> init(CharSequence clOrdID, F fields, OrderListener<F, Order<F>>... listeners) {
		newRequest.init(clOrdID);
		replaceRequest.reset();
		cancelRequest.reset();
		this.fields = fields;
		avgPx = cumQty = 0;
		this.listeners.clear();
		if (listeners != null) {
			addAll(this.listeners, listeners);
		}
		orderID = idgen.getOrderID();
		transactTime = incoming.transactTime;
		orderTime = incoming.getTime();
		end(l->l.onNewRequest(this));
		return this;
	}

	public void fill(final long qty, final double px) {
		begin();
		double totalValue = qty * px + cumQty * avgPx;
		avgPx = totalValue / (qty + cumQty);
		cumQty += qty;
		if (cumQty >= fields.getOrderQty()) {
			terminalOrdStatus = OrdStatus.Filled;
		}
		if (parent != null) {
			parent.fill(qty, px);
		}
		transactTime = incoming.transactTime;
		end(l->l.onExecutionReport(this, ExecType.Trade, qty, px));

		//separate transaction
		if (replaceRequest.isPending() && replaceRequest.getQty() <= cumQty && replaceRequest.getQty() != fields.getOrderQty()) {
			replaceRequest.accept(); // https://www.onixs.biz/fix-dictionary/4.2/app_d12.html
		} else if (cumQty >= fields.getOrderQty()) {
			if (cancelRequest.isPending()) {
				cancelRequest.reject();
			}
			if (replaceRequest.isPending()) {
				replaceRequest.reject();
			}
		}
	}

	public void replace(F fields) throws NotEnoughQtyException {
		begin();
		if (parent != null && fields.getOrderQty() - this.fields.getOrderQty() > parent.getAvailableQty()) { //callers should check this
			throw new NotEnoughQtyException();
		}
		this.fields = fields;
		if (cumQty >= fields.getOrderQty()) {
			terminalOrdStatus = OrdStatus.Filled;
		} else if (fields.hasExecInst(Suspend)) {
			terminalOrdStatus = OrdStatus.Suspended;
		} else {
			orderTime = incoming.getTime();
		}
		transactTime = incoming.transactTime;
		end(l->l.onExecutionReport(this, Replaced, 0, 0));

		//separate transaction
		if (cancelRequest.isPending() && cumQty >= fields.getOrderQty()) {
			cancelRequest.reject();
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
		terminalOrdStatus = status;
		transactTime = incoming.transactTime;
		end(l->l.onExecutionReport(this, execType, 0, 0));

		//separate transaction
		if (cancelRequest.isPending()) {
			cancelRequest.accept();
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
		if (parent != null) {
			parent.addWorkingQtyChange(getWorkingQty() - origWorkingQty); //can be negative
		}
	}

	@Override
	protected void setParent(Parental parent) {
		this.parent = parent;
	}

	@Override
	public long getWorkingQty() {
		if (terminalOrdStatus != null) {
			return 0;
		}
		long potentialOrderQty = max(replaceRequest.isPending() ? replaceRequest.getQty() : 0, fields.getOrderQty());
		return max(0, potentialOrderQty - cumQty);
	}

	@Override
	public void requestCancel() {
		cancelRequest.init(null);
	}

	@Override
	public Collection<OrderListener<F, Order<F>>> listeners() {
		return listeners;
	}

	@Override
	public View view() {
		return view;
	}
	
	public class View {
		//getters here are accessible via Parental without extra code in Parental

		public OrdStatus getOrdStatus() {
			return
				cancelRequest.isPending()  ? cancelRequest.getStatus() :
				replaceRequest.isPending() ? replaceRequest.getStatus() :
				terminalOrdStatus != null  ? terminalOrdStatus :
				cumQty > 0                 ? OrdStatus.PartiallyFilled :
			                                     newRequest.getStatus();
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
			return terminalOrdStatus != null ? 0 : fields.getOrderQty() - cumQty;
		}

		public boolean isWorking() {
			return terminalOrdStatus == null;
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

		public long getOrderTime() {
			return orderTime;
		}

		public Parental getParent() {
			return parent;
		}
	}
}
