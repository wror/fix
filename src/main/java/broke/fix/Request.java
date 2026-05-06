package broke.fix;

import broke.fix.dto.OrdStatus;
import broke.fix.misc.FixException.Reason;
import broke.fix.misc.OrderListener;

import java.util.function.Consumer;

public abstract class Request {
	public final CharSequence origClOrdID, clOrdID;
	public final Order<?> order;

	public enum Status { Pending, Accepted, Rejected }

	protected abstract long getRequestedOrderQty();
	protected abstract OrdStatus getPendingOrdStatus();
	protected abstract void onFill();
	public abstract void accept();
	public abstract void reject(Reason reason);

	public Request(Order<?> order, CharSequence origClOrdID, CharSequence clOrdID) {
		this.order = order;
		this.origClOrdID = origClOrdID;
		this.clOrdID = clOrdID;
		order.onRequestChange(this, Status.Pending);
	}

	protected final void setStatus(Status newStatus) {
		order.onRequestChange(this, newStatus);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected final void endTransaction(Consumer<OrderListener> listenerCall) {
		((Order)order).endTransaction(listenerCall);
	}
}
