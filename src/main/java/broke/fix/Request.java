package broke.fix;

import broke.fix.Order;
import broke.fix.dto.OrdStatus;
import broke.fix.dto.ExecType;
import broke.fix.misc.FixFields;

import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class Request<F extends FixFields> {
	private final static Logger log = LogManager.getLogger();
	protected final Order<F> order;
	protected StringBuffer clOrdID = new StringBuffer(16);
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
		this.clOrdID.setLength(0);
		this.clOrdID.append(clOrdID != null ? clOrdID : order.idgen.getClOrdID());
	}

	protected void reset() {
		status = null;
	}

	protected void accept() { //protected b/c callers should use cancel() on the order instead of cancelRequest.accept()
		if (!isPending()) {
			log.warn("Accept of {} request on {}", status, order);
		}
		status = Status.Accepted;
		StringBuffer replacedClOrdID = order.newRequest.clOrdID;
		order.newRequest.clOrdID = this.clOrdID;
		this.clOrdID = replacedClOrdID; //so its memory can be used next time
	}

	public void reject() {
		if (!isPending()) {
			log.warn("Reject of {} request on {}", status, order);
		}
		status = Status.Rejected;
	}

	public CharSequence getClOrdID() {
		return clOrdID;
	}

	public CharSequence getOrigClOrdID() {
		return order.view().getClOrdID();
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
