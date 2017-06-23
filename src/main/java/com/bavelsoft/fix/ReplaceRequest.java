package com.bavelsoft.fix;

import static com.bavelsoft.fix.OrdStatus.PendingReplace;

public class ReplaceRequest extends Request {
	private Object fields;

        public ReplaceRequest(Order order, Object fields) {
                super(order);
                this.fields = fields;
        }

        OrdStatus getPendingOrdStatus() {
                return PendingReplace;
        }

        protected void acceptImpl() {
                order.replace(fields);
        }
}


