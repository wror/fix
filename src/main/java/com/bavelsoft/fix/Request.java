package com.bavelsoft.fix;

import com.bavelsoft.fix.OrdStatus;
import com.bavelsoft.fix.ExecType;

public abstract class Request {
        public final Order<?> order;

        private CharSequence clOrdID, origClOrdID;
	enum Status { Pending, Accepted, None }
	private Status status;

        public Request(Order<?> order) {
                this.order = order;
		reset();
        }

	public void reset() {
		status = Status.None;
		clOrdID = null;
		origClOrdID = null;
	}

	public Request init(CharSequence clOrdID) {
		status = Status.Pending;
		this.clOrdID = clOrdID;
		this.origClOrdID = order.replaceRequest.getClOrdID();
		if (this.origClOrdID == null) {
			this.origClOrdID = order.newRequest.getClOrdID();
		}
		return this;
	}

        public void reject() {
		reset();
        }

        public void accept() {
		status = Status.Accepted;
        }

	public boolean isPending() {
		return status == Status.Pending;
	}

	public boolean isAccepted() {
		return status == Status.Accepted;
	}

	public CharSequence getClOrdID() {
		return clOrdID;
	}

	public CharSequence getOrigClOrdID() {
		return origClOrdID;
	}
}
