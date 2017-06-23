package com.bavelsoft.fix;

import static com.bavelsoft.fix.OrdStatus.Rejected;
import static com.bavelsoft.fix.OrdStatus.DoneForDay;
import static com.bavelsoft.fix.OrdStatus.Canceled;

public class Order {
        private Object fields;
        private long cumQty, leavesQty, orderID;
        private double avgPx;
        Request lastRequest;
        OrdStatus terminalStatus;

        public void reject() {
                terminalStatus = Rejected;
                leavesQty = 0;
        }

        public void done() {
                terminalStatus = DoneForDay;
                leavesQty = 0;
        }

        public void cancel() {
                cancel(leavesQty);
        }

        public void cancel(long qtyChange) {
                leavesQty -= qtyChange;
		if (leavesQty <= 0)
			terminalStatus = Canceled;
        }

        public void replace(Object fields) {
                this.fields = fields;
        }

        public void fill(Execution x) {
                avgPx = x.getNewAvgPx(this);
                cumQty += x.getQty();
                leavesQty -= x.getQty();
        }

        public void request(Request request) {
                request.previousRequest = lastRequest;
                lastRequest = request;
        }

	public long getCumQty() {
		return cumQty;
	}

	public double getAvgPx() {
		return avgPx;
	}

	public long getOrderQty() {
		return 0; //TODO should come from fields
	}
}
