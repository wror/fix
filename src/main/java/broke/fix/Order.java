package broke.fix;

import broke.fix.dto.CxlRejReason;
import broke.fix.dto.ExecType;
import broke.fix.dto.OrdStatus;
import broke.fix.misc.FixFields;
import broke.fix.misc.IncomingContext;
import broke.fix.misc.IdGenerator;
import broke.fix.misc.OrderListener;
import broke.fix.misc.NotEnoughQtyException;
import broke.fix.CancelRequest;
import broke.fix.NewRequest;
import broke.fix.OrderComposite;
import broke.fix.ReplaceRequest;
import broke.fix.Request;
import java.util.Collection;
import java.util.ArrayList;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static java.util.Arrays.asList;
import static java.util.Collections.addAll;
import static java.lang.Long.max;

import static broke.fix.dto.ExecInst.Suspend;

public class Order<F extends FixFields> extends OrderComponent<F, Order<F>> {
	private final static Logger log = LogManager.getLogger();
	private OrderComposite parent;
	private F fields;
	private long cumQty, origWorkingQty, transactTime, orderTime, internalOrderID;
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
		internalOrderID = idgen.getOrderID();
		replaceRequest.reset();
		cancelRequest.reset();
		this.fields = fields;
		avgPx = cumQty = 0;
		this.listeners.clear();
		if (listeners != null) {
			addAll(this.listeners, listeners);
		}
		transactTime = incoming.transactTime;
		orderTime = incoming.getTime();
		newRequest.init(clOrdID);
		end(l->l.onNewRequest(this));
		return this;
	}

	public void fill(final long qty, final double px) {
		begin();
		double totalValue = qty * px + cumQty * avgPx;
		avgPx = totalValue / (qty + cumQty);
		cumQty += qty;
		if (parent != null) {
			parent.fill(qty, px);
		}
		transactTime = incoming.transactTime;
		end(l->l.onExecutionReport(this, ExecType.Trade, qty, px));

		//separate transaction
		handleReplaceRequestIfFilled();
	}

	public void replace(F fields) throws NotEnoughQtyException {
		begin();
		if (!canReplace(fields)) { //callers should check this
			throw new NotEnoughQtyException();
		}
		this.fields = fields;
		if (cumQty < fields.getOrderQty()) {
			orderTime = incoming.getTime();
		}
		transactTime = incoming.transactTime;
		end(l->l.onExecutionReport(this, ExecType.Replaced, 0, 0));

		//separate transaction
		//relevant for this being an unsolicted replace
		handleReplaceRequestIfFilled();
	}

	private void handleReplaceRequestIfFilled() {
		//for flexibility, we'll let the publishing layer decide what to do with a pending cancel, in all cases
		if (replaceRequest.isPending() && cumQty >= replaceRequest.getQty()) {
			if (replaceRequest.getQty() != fields.getOrderQty()) {
				replaceRequest.accept(); // https://www.onixs.biz/fix-dictionary/4.2/app_d12.html
			} else if (cumQty >= fields.getOrderQty()) {
				replaceRequest.reject();
			}
		}
	}

	public boolean canReplace(F fields) {
		return parent == null || fields.getOrderQty() - this.fields.getOrderQty() <= parent.getAvailableQty();
	}

	public void cancel() {
		terminate(OrdStatus.Canceled, ExecType.Canceled);
	}

	public void done() {
		terminate(OrdStatus.DoneForDay, ExecType.DoneForDay);
	}

	protected void terminate(final OrdStatus status, final ExecType execType) {
		if (replaceRequest.isPending()) {
			replaceRequest.reject();
		}
		//separate transaction
		begin();
		terminalOrdStatus = status;
		transactTime = incoming.transactTime;
		end(l->l.onExecutionReport(this, execType, 0, 0));
	}

	protected void begin() {
		origWorkingQty = getWorkingQty();
	}

	protected void end(Consumer<OrderListener> listenerCall) {
		if (parent != null) {
			parent.addWorkingQtyChange(getWorkingQty() - origWorkingQty); //can be negative
		}
		for (OrderListener<F, Order<F>> listener : listeners) {
			try {
				listenerCall.accept(listener);
			} catch (RuntimeException e) {
				log.warn("Listener exception", e);
			}
		}
	}

	@Override
	protected void setParent(OrderComposite parent) {
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
	public void requestReplace(F newFields) {
		replaceRequest.init(null, newFields);
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
		//getters here are accessible via OrderComposite without extra code there

		public OrdStatus getOrdStatus() {
			return
				terminalOrdStatus != null      ? terminalOrdStatus :
				cancelRequest.isPending()      ? cancelRequest.getStatus() :
				replaceRequest.isPending()     ? replaceRequest.getStatus() :
				fields.hasExecInst(Suspend)    ? OrdStatus.Suspended :
				cumQty >= fields.getOrderQty() ? OrdStatus.Filled :
				cumQty > 0                     ? OrdStatus.PartiallyFilled :
			                                         newRequest.getStatus();
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

		public long getInternalOrderID() {
			return internalOrderID;
		}

		public CharSequence getOrderID() {
			return newRequest.getOrderID();
		}

		public boolean isPending() {
			return newRequest.isPending();
		}

		public CharSequence getClOrdID() {
			return newRequest.getClOrdID();
		}

		public CharSequence getOptimisticClOrdID() {
			return replaceRequest.isPending() ? replaceRequest.getClOrdID() : newRequest.getClOrdID();
		}

		public long getTransactTime() {
			return transactTime;
		}

		public long getOrderTime() {
			return orderTime;
		}

		public boolean isRoot() {
			return parent == null;
		}

		public OrderComposite getParent() {
			return parent;
		}
	}
}
