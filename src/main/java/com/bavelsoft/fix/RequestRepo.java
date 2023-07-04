package com.bavelsoft.fix;

import java.util.Map;
import java.util.function.Consumer;

public class RequestRepo {
	private final Map<CharSequence, Request> map;
	Consumer<Order> invariants = o->{};

	private static Request TOO_LATE_REQUEST = new Request(null) {};

	public RequestRepo(Map<CharSequence, Request> map) {
		this.map = map;
	}

	public Request get(CharSequence clOrdID) {
		return map.get(clOrdID);
	}

	boolean requestNew(Order<?> order, CharSequence clOrdID) {
		return request(order.newRequest, clOrdID);
	}

	public boolean requestCancel(Order<?> order, CharSequence clOrdID) {
		return request(order.cancelRequest, clOrdID);
	}

	public <T> RequestReplace requestReplace(Order<T> order, CharSequence clOrdID) {
		invariants.accept(order);
		if (request(order.replaceRequest, clOrdID)) {
			return order.replaceRequest;
		} else {
			return null;
		}
	}

	private boolean request(Request request, CharSequence clOrdID) {
		request.init(clOrdID);
		map.put(clOrdID, request);
		return true;
	}

	void remove(Request request) {
		map.put(request.getClOrdID(), TOO_LATE_REQUEST);
	}
}
