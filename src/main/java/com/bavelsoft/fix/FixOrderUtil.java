package com.bavelsoft.fix;

import com.bavelsoft.fix.OrdStatus;

public class FixOrderUtil<F> {
	/**
	 * @return whether the sell-side triggered this call in an expected way
	 */
	public static boolean exec(Order<?> order, ExecType execType, CharSequence clOrdID) {
		switch (execType) {
			case Rejected:
				if (order.getOrdStatus() == OrdStatus.PendingNew) {
					order.newRequest.reject();
					return true;
				} else {
					order.cancel();
					return false;
				}
			case New:
				return accept(order.newRequest, clOrdID);
			case Replaced:
				return accept(order.replaceRequest, clOrdID);
			case Canceled:
				if (!accept(order.cancelRequest, clOrdID)) {
					order.cancel();
				}
				//TODO return false if clOrdID wasn't the last acked clOrdID
				return true;
			case DoneForDay:
				order.done();
				return true;
			case PendingNew:
			case PendingReplace:
			case PendingCancel:
				return true; //ignore
			default:
				return false;
		}
	}

	private static boolean accept(Request request, CharSequence clOrdID) {
		if (request.isPending()) {
			request.accept();
			return request.getClOrdID().equals(clOrdID);
		} else {
			return false;
		}
	}
}
