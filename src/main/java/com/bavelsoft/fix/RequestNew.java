package com.bavelsoft.fix;

import com.bavelsoft.fix.OrdStatus;
import com.bavelsoft.fix.ExecType;
import com.bavelsoft.fix.Request;
import com.bavelsoft.fix.Order;

public class RequestNew extends Request {
	public RequestNew(Order<?> order) {
	        super(order);
	}

        protected OrdStatus getStatus() {
                return isAccepted() ? OrdStatus.New : isPending() ? OrdStatus.PendingNew : OrdStatus.Rejected;
        }
}
