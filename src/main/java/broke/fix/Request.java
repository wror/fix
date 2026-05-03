package broke.fix;

import broke.fix.dto.CxlRejReason;
import broke.fix.dto.OrdStatus;
import broke.fix.misc.FixFields;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class Request<O extends Order<F>, F extends FixFields> {
	private final static Logger log = LogManager.getLogger();
	private final CharSequence origClOrdID;
	private final O order;
	public enum Status { Pending, Accepted, Rejected }
	private Status status;
	private CharSequence clOrdID;

	protected abstract OrdStatus getPendingStatus();
	protected abstract void onFill();
	public abstract void accept();
	public abstract void reject(Object reason);

	public Request(CharSequence clOrdID, O order) {
		status = Status.Pending;
		this.order = order;
		this.clOrdID = clOrdID;
		this.origClOrdID = order.getClOrdID();
		order.onRequestChange(this);
	}

	protected void setStatus(Status newStatus) {
		//could happen because request was still available from UpstreamRepository
		if (status == (newStatus == Status.Accepted ? Status.Rejected : Status.Accepted)) {
			String message = "Attempted transition from "+ status +" to "+ newStatus;
			log.warn(message);
			throw new RuntimeException(message);
		}
		status = newStatus;
		order.onRequestChange(this);
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

	public O getOrder() {
		return order;
	}

	public long getQty() {
		return order.getFields().getOrderQty();
	}
}
