package m15xb7.fix.service;

import m15xb7.fix.Order;
import m15xb7.fix.ParentOrder;
import m15xb7.fix.misc.ExecType;
import m15xb7.fix.misc.IdGenerator;
import m15xb7.fix.misc.OrderListener;
import m15xb7.fix.misc.SimplePool;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.HashMap;

public class PassthroughListeners<F, R> {
	private final Map<Order, Order> clientOrders = new HashMap<>();
	private final Map<ParentOrder, Order> exchangeOrders = new HashMap<>();
	private final IdGenerator idgen;
	private final Map<CharSequence, Order> exchangeOrderByClOrdID;
	private final OrderListener exchangeOrderPublisher;
	private final SimplePool<Order<R, R>> pool = new SimplePool<>(new ArrayDeque<>(), Order::new, 10);

	public PassthroughListeners(IdGenerator idgen, Map<CharSequence, Order> exchangeOrderByClOrdID, OrderListener exchangeOrderPublisher) {
		this.idgen = idgen;
		this.exchangeOrderByClOrdID = exchangeOrderByClOrdID;
		this.exchangeOrderPublisher = exchangeOrderPublisher;
	}

	public void slicePassthrough(Order<R, R> clientOrder, ParentOrder<R> parentOrder) {
		parentOrder.addListener(clientOrderListener);
		Order exchangeOrder = pool.acquire().init(idgen, parentOrder, clientOrder.view.getFields(), clientOrder.view.getLeavesQty());
		exchangeOrder.addListener(exchangeOrderPublisher);
		exchangeOrder.addListener(exchangeOrderListener);
		exchangeOrderByClOrdID.put(exchangeOrder.newRequest.getClOrdID(), exchangeOrder);
		exchangeOrders.put(parentOrder, exchangeOrder);
		clientOrders.put(exchangeOrder, clientOrder);
	}

	//TODO avoid new'ing up these stateless listeners (and WorkingQtyListener), which we're currently doing just for the sake of the generic types
	private OrderListener<R, ParentOrder<R>> clientOrderListener = new OrderListener<>() {
		@Override
		public void onCancelRequest(ParentOrder<R> parentOrder) {
			exchangeOrders.get(parentOrder).requestCancel();
		}

		@Override
		public void onReplaceRequest(ParentOrder<R> parentOrder) {
			Order exchangeOrder = exchangeOrders.get(parentOrder);
			Order clientOrder = clientOrders.get(exchangeOrder);
			exchangeOrder.requestReplace(clientOrder.replaceRequest.getFields(), clientOrder.replaceRequest.getQty());
		}
	};

	private OrderListener<F, Order<F, R>> exchangeOrderListener = new OrderListener<>() {
		@Override
		public void onFill(Order<F, R> exchangeOrder, long qty, double px) {
			clientOrders.get(exchangeOrder).fill(qty, px);
		}

		@Override
		public void onNonFillExecutionReport(Order<F, R> exchangeOrder, ExecType execType) {
			Order clientOrder = clientOrders.get(exchangeOrder);
			switch (execType) {
				case New:
					clientOrder.newRequest.accept(null);
					break;
				case Rejected:
					clientOrder.newRequest.reject();
					break;
				case Replaced:
					clientOrder.replaceRequest.accept();
					break;
				case DoneForDay:
					clientOrder.done();
					break;
				case Canceled:
					clientOrder.cancel();
					break;
			}
		}

		@Override
		public void onCancelReject(Order<F, R> exchangeOrder) {
			clientOrders.get(exchangeOrder).cancelRequest.reject();
		}

		public void onReplaceReject(Order exchangeOrder) {
			clientOrders.get(exchangeOrder).replaceRequest.reject();
		}
	};
}
