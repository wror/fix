package com.bavelsoft.fix;

import static com.bavelsoft.fix.OrdStatus.New;
import static com.bavelsoft.fix.OrdStatus.PartiallyFilled;
import static com.bavelsoft.fix.OrdStatus.Filled;

public class StatusUtil {
        public static OrdStatus getOrdStatus(Order order) {
                if (order.terminalStatus != null)
                        return order.terminalStatus;
                else if (order.getCumQty() >= order.getOrderQty())
                        return Filled;
                else if (getPendingOrdStatus(order) != null)
                        return getPendingOrdStatus(order);
                else if (order.getCumQty() > 0)
                        return PartiallyFilled;
                else
                        return New;
        }

        private static OrdStatus getPendingOrdStatus(Order order) {
                if (order.lastRequest == null)
                        return null;
                Request pendingRequest = order.lastRequest.getLastPending();
                if (pendingRequest == null)
                        return null;
                return pendingRequest.getPendingOrdStatus();
        }
}


