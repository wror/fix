package com.bavelsoft.fix;

import java.util.LinkedList;

public class PendingRequests {
	private LinkedList<Request> requests = new LinkedList<>();

        public OrdStatus getOrdStatus() {
		return requests.isEmpty() ? null : requests.getLast().getPendingOrdStatus();
        }

        public long getMaxOrderQty() {
		return requests.stream().mapToLong(x->x.getPendingOrderQty()).max().orElse(0);
        }

	public void addOrRemove(Request request) {
		if (request.isPending()) {
			requests.add(request);
		} else {
			requests.remove(request);
		}
	}
}
