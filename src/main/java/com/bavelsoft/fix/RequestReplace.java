package com.bavelsoft.fix;

import com.bavelsoft.fix.OrdStatus;
import com.bavelsoft.fix.ExecType;
import com.bavelsoft.fix.Request;
import com.bavelsoft.fix.Order;

public class RequestReplace<F> extends Request {
	public F pendingFields;
	public long pendingOrderQty;

        public RequestReplace(Order<F> order) {
                super(order);
	}

	public long getWorkingQty() {
		long leavesQty = order.getLeavesQty();
		if (isPending()) {
			return Math.max(leavesQty, pendingOrderQty - order.getCumQty());
		} else {
			return leavesQty;
		}
	}

	@Override
        public void accept() {
		super.accept();
                ((Order<F>)order).replace(pendingFields, pendingOrderQty);
        }

        public OrdStatus getStatus() {
                return isPending() ? OrdStatus.PendingReplace : null;
        }
}
