package com.bavelsoft.fix;

public abstract class Request {
        private String clOrdID;
        private boolean isRejected, isAccepted;
        protected Order order;
        protected Request previousRequest;

        Request(Order order) {
                this.order = order;
        }

        public void accept() {
                isAccepted = true;
                acceptImpl();
        }

        public void reject() {
                isRejected = true;
                rejectImpl();
        }

        public Request getLastPending() {
                Request request = this;
                while (request != null && (request.isRejected || request.isAccepted))
                        request = request.previousRequest;
                return request;
        }

        protected void acceptImpl() {}
        protected void rejectImpl() {}
        abstract OrdStatus getPendingOrdStatus();
}


