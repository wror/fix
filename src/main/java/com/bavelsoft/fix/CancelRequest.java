package com.bavelsoft.fix;

import static com.bavelsoft.fix.OrdStatus.PendingCancel;

public class CancelRequest extends Request {
	public CancelRequest(Order order) {
		super(order);
	}

        OrdStatus getPendingOrdStatus() {
                return PendingCancel;
        }

        protected void acceptImpl() {
                order.cancel();
        }
}

