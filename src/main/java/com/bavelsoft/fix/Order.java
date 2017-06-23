package com.bavelsoft.fix;

import com.bavelsoft.fix.OrdStatus;

public class Order {
        private Object fields;
	private String orderID;
        private double avgPx;
        private long orderQty, cumQty, leavesQty, pendingOrderQty;
	private OrdStatus terminalOrdStatus, pendingOrdStatus;
	private PendingRequests pendingRequests = new PendingRequests();

        public void fill(Execution x) {
		double totalValue = x.getQty() * x.getPrice() + cumQty * avgPx;
                avgPx = totalValue / (cumQty + x.getQty());
                cumQty += x.getQty();
                leavesQty -= x.getQty();
        }

        public void replace(Object newFields, long requestedOrderQty) {
		long newOrderQty = Math.max(requestedOrderQty, cumQty);
		leavesQty += newOrderQty - orderQty;
                orderQty = newOrderQty;
                fields = newFields;
        }

        public void cancel() {
                cancel(leavesQty);
        }

        public void cancel(long qtyChange) {
                leavesQty -= qtyChange;
		if (leavesQty <= 0)
			terminalOrdStatus = OrdStatus.Canceled;
        }

        public OrdStatus getOrdStatus(Order order) {
		return
                    terminalOrdStatus != null ? terminalOrdStatus :
                     pendingOrdStatus != null ? pendingOrdStatus :
                           cumQty >= orderQty ? OrdStatus.Filled :
                                   cumQty > 0 ? OrdStatus.PartiallyFilled
                                              : OrdStatus.New;
        }

	public void updateWithRequest(Request request) {
		pendingRequests.addOrRemove(request);
		pendingOrdStatus = pendingRequests.getOrdStatus();
		pendingOrderQty = pendingRequests.getMaxOrderQty();
	}

        public void done() {
                leavesQty = 0;
                terminalOrdStatus = OrdStatus.DoneForDay;
        }

        public void reject() {
                leavesQty = 0;
                terminalOrdStatus = OrdStatus.Rejected;
        }

	public long getOrderQty() {
		return orderQty;
	}

	public long getCumQty() {
		return cumQty;
	}

	public long getLeavesQty() {
		return leavesQty;
	}

	public String getOrderID() {
		return orderID;
	}

	public double getAvgPx() {
		return avgPx;
	}
}
