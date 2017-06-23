package com.bavelsoft.fix;

import com.bavelsoft.fix.OrdStatus;
import com.bavelsoft.fix.ExecType;
import com.bavelsoft.fix.Request;
import com.bavelsoft.fix.Order;

public class CancelRequest extends Request {
	public CancelRequest(Order order, String clOrdID) {
		super(order, clOrdID);
	}

	@Override
        protected void onAccept() {
                getOrder().cancel();
        }

	@Override
        protected OrdStatus getPendingOrdStatus() {
                return OrdStatus.PendingCancel;
        }

	@Override
        protected ExecType getPendingExecType() {
                return ExecType.PendingCancel;
        }

	@Override
        protected ExecType getAcceptedExecType() {
                return ExecType.Canceled;
        }
}

