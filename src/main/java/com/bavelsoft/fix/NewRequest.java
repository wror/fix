package com.bavelsoft.fix;

import static com.bavelsoft.fix.OrdStatus.PendingNew;

public class NewRequest extends Request {
        public NewRequest(Order order) {
                super(order);
        }

        OrdStatus getPendingOrdStatus() {
                return PendingNew;
        }

        protected void rejectImpl() {
                order.reject();
        }
}


