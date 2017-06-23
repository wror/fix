package com.bavelsoft.fix;

import com.bavelsoft.fix.OrdStatus;
import com.bavelsoft.fix.ExecType;
import com.bavelsoft.fix.Request;
import com.bavelsoft.fix.Order;

public class ReplaceRequest extends Request {
	private Object pendingFields;
	private long pendingOrderQty;

        public ReplaceRequest(Order order, String clOrdID, Object pendingFields, long pendingOrderQty) {
                super(order, clOrdID);
                this.pendingFields = pendingFields;
                this.pendingOrderQty = pendingOrderQty;
        }

	@Override
        protected void onAccept() {
                getOrder().replace(pendingFields, pendingOrderQty);
        }

	@Override
        protected OrdStatus getPendingOrdStatus() {
                return OrdStatus.PendingReplace;
        }

	@Override
        protected ExecType getPendingExecType() {
                return ExecType.PendingReplace;
        }

	@Override
        protected ExecType getAcceptedExecType() {
                return ExecType.Replaced;
        }

	@Override
	public long getPendingOrderQty() {
		return pendingOrderQty;
	}
}
