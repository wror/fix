package m15xb7.fix.service;

import m15xb7.fix.Order;
import m15xb7.fix.Request;
import m15xb7.fix.RequestReplace;
import m15xb7.fix.misc.CxlRejResponseTo;
import m15xb7.fix.misc.ExecType;
import m15xb7.fix.misc.IdGenerator;
import m15xb7.fix.misc.OrdStatus;

import java.util.Map;
import java.util.HashMap;

//TODO listener for publishing requests

public class DownstreamHandler {
	private Map<String, Order> orderByClOrdID = new HashMap<>(); //TODO dependency injection or something
	private Map<String, Order> orderByOrderID = new HashMap<>();
	private IdGenerator idgen = new IdGenerator();

	/**
	 * @return whether the sell-side triggered this call in an expected way
	 */
	public boolean handleExecutionReport(ExecType execType, long transactTime, CharSequence orderID, CharSequence clOrdID) {
		idgen.incomingTransactTime = transactTime;
		Order order = getOrder(orderID, clOrdID);
		if (order == null) {
			//TODO log
			return false;
		}
		switch (execType) {
			case Rejected:
				if (order.view.getOrdStatus() == OrdStatus.PendingNew) {
					order.newRequest.reject();
					return true;
				} else {
					order.cancel();
					return false;
				}
			case New:
				order.newRequest.accept(orderID);
			case Trade:
				if (order.newRequest.isPending()) {
					order.newRequest.accept(orderID);
				}
				//order.fill() //TODO
			case Replaced:
				RequestReplace replace = order.replaceRequest;
				if (replace.isPending()) {
					replace.accept();
					return replace.getClOrdID().equals(clOrdID);
				} else {
					return false;
				}
			case Canceled:
				order.cancel();
				//TODO return false if clOrdID wasn't the last acked clOrdID?
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

	//TODO share with UpstreamHandler?
	public Order getOrder(CharSequence orderID, CharSequence clOrdID) {
		if (orderID != null) {
			Order order = orderByOrderID.get(orderID);
			if (order != null) {
				return order;
			}
		}
		if (clOrdID != null) {
			Order order = orderByClOrdID.get(clOrdID);
			if (clOrdID != null) {
				return order;
			}
		}
		return null;
	}

	public void handleOrderCancelReject(CxlRejResponseTo responseTo, CharSequence orderID, CharSequence clOrdID) {
		Order order = getOrder(orderID, clOrdID);
		if (order == null) {
			//TODO log
			return;
		}
		if (responseTo == CxlRejResponseTo.Cancel) {
			order.cancelRequest.reject();
		} else if (responseTo == CxlRejResponseTo.Replace) {
			order.replaceRequest.reject();
		} else {
			//TODO log
		}
	}
}
