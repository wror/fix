package m15xb7.fix;

import m15xb7.fix.misc.OrdStatus;
import m15xb7.fix.misc.ExecType;

public abstract class Request<F> {
	public final Order<F, ?> order;
	private CharSequence clOrdID, origClOrdID; //TODO set origClOrdID
	enum Status { Pending, Accepted, Rejected }
	Status status;

	public Request(Order<F, ?> order) {
		this.order = order;
	}

	protected abstract OrdStatus getStatus();

	public void init(CharSequence clOrdID) {
		status = Status.Pending;
		if (order.parent != null) { //order sender sets "optimistic" clOrdID
			order.clOrdID = clOrdID;
		}
		this.clOrdID = clOrdID;
	}

	public void reject() {
		status = Status.Rejected;
	}

	void accept() {
		status = Status.Accepted;
		order.clOrdID = clOrdID;
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
