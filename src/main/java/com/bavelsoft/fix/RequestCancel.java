package com.bavelsoft.fix;

import com.bavelsoft.fix.OrdStatus;
import com.bavelsoft.fix.ExecType;
import com.bavelsoft.fix.Request;
import com.bavelsoft.fix.Order;

public class RequestCancel extends Request {
	public RequestCancel(Order<?> order) {
		super(order);
	}

	@Override
        public void accept() {
		super.accept();
               	order.cancel();
        }

        protected OrdStatus getStatus() {
                return isPending() ? OrdStatus.PendingCancel : null;
        }
}

