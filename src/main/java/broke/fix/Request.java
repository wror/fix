package broke.fix;

import broke.fix.Order;
import broke.fix.misc.OrdStatus;
import broke.fix.misc.ExecType;

import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class Request<F> {
	private final static Logger log = LogManager.getLogger();
	protected final Order<F> order;
	protected CharSequence clOrdID, origClOrdID;
	private enum Status { Pending, Accepted, Rejected }
	private Status status;

	public Request(Order<F> order) {
		this.order = order;
	}

	public abstract OrdStatus getStatus();

	protected void init(CharSequence clOrdID) {
		if (isPending()) {
			log.warn("Duplicate request on {}", order);
		}
		status = Status.Pending;
		if (!order.view().isRoot()) { //order sender sets "optimistic" clOrdID
			origClOrdID = order.newRequest.clOrdID;
			order.newRequest.clOrdID = clOrdID;
		}
		this.clOrdID = clOrdID;
	}

	public void reset() {
		status = null;
	}

	protected void reject() {
		if (!isPending()) {
			log.warn("Reject of {} request on {}", status, order);
		}
		status = Status.Rejected;
		if (!order.view().isRoot()) { //rollback "optimistic" clOrdID
			order.newRequest.clOrdID = origClOrdID;
		}
	}

	protected void accept() {
		if (!isPending()) {
			log.warn("Accept of {} request on {}", status, order);
		}
		status = Status.Accepted;
		order.newRequest.clOrdID = clOrdID;
	}

	public CharSequence getClOrdID() {
		return clOrdID;
	}

	public CharSequence getOrigClOrdID() {
		return origClOrdID;
	}

	public boolean isPending() {
		return status == Status.Pending;
	}

	public boolean isAccepted() {
		return status == Status.Accepted;
	}

	public boolean isRejected() {
		return status == Status.Rejected;
	}
}
