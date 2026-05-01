package broke.fix;

import broke.fix.dto.CxlRejReason;
import broke.fix.dto.ExecType;
import broke.fix.dto.OrdStatus;
import broke.fix.misc.FixFields;
import broke.fix.misc.NotEnoughQtyException;
import broke.fix.misc.OrderListener;

import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class Request<F extends FixFields> {
	private final static Logger log = LogManager.getLogger();
	private final CharSequence origClOrdID;
	CharSequence clOrdID;
	protected final Order<F> order;
	protected enum Status { Pending, Accepted, Rejected }
	protected Status status;
	protected abstract OrdStatus getPendingStatus();
	protected abstract void onFill();
	protected abstract void onAccept();
	protected abstract void onReject(Object reason);

	public Request(CharSequence origClOrdID, CharSequence clOrdID, Order<F> order) {
		status = Status.Pending;
		this.origClOrdID = origClOrdID;
		this.clOrdID = clOrdID;
		this.order = order;
	}

	public void accept() {
		setStatus(Status.Accepted);
		order.requests.remove(this);
		order.onAccept(this);
		onAccept();
	}

	public void reject(Object reason) {
		setStatus(Status.Rejected);
		order.requests.remove(this);
		order.onReject(this);
		onReject(reason);
	}

	private void setStatus(Status newStatus) {
		//could happen because request was still available from UpstreamRepository
		if (status == (newStatus == Status.Accepted ? Status.Rejected : Status.Accepted)) {
			String message = "Attempted transition from "+ status +" to "+ newStatus;
			log.warn(message);
			throw new RuntimeException(message);
		}
		status = newStatus;
	}

	public Status getStatus() {
		return status;
	}

	public static final CxlRejReason cxlRejReason(Object reason) {
		if (reason instanceof CxlRejReason) {
			return (CxlRejReason)reason;
		} else {
			return CxlRejReason.Other;
		}
	}

	public final CharSequence getClOrdID() {
		return clOrdID;
	}

	public final CharSequence getOrigClOrdID() {
		return origClOrdID;
	}

	public Order<F> getOrder() {
		return order;
	}

	public long getQty() {
		return order.getFields().getOrderQty();
	}

	//TODO maybe move Request out of the package with Order, use a different solution for the following

	protected final void replace(F fields) throws NotEnoughQtyException {
		order.replace(fields);
	}

	protected final void endTransaction(Consumer<OrderListener<Order<F>>> listenerCall) {
		order.endTransaction(listenerCall);
	}

	protected final void terminate(final OrdStatus ordStatus, final ExecType execType, Object reason) {
		order.terminate(ordStatus, execType, reason);
	}
}
