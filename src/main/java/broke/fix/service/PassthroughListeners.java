package broke.fix.service;

import broke.fix.Order;
import broke.fix.Parental;
import broke.fix.misc.CxlRejReason;
import broke.fix.misc.ExecType;
import broke.fix.misc.IdGenerator;
import broke.fix.misc.OrderListener;
import broke.fix.misc.SimpleOrderRepo;

import javax.inject.Inject;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.HashMap;

public class PassthroughListeners<F, R> {
	private final IdGenerator idgen;
	private final OrderListener exchangeOrderPublisher;
	private final SimpleOrderRepo<F, Order<F>> repo;
	private final Map<Order, Order> clientOrders = new HashMap<>();
	private final Map<Parental, Order> exchangeOrders = new HashMap<>();

	@Inject
	public PassthroughListeners(IdGenerator idgen, SimpleOrderRepo<F, Order<F>> repo, OrderListener exchangeOrderPublisher) {
		this.idgen = idgen;
		this.repo = repo;
		this.exchangeOrderPublisher = exchangeOrderPublisher;
	}

	public void slicePassthrough(Order<R> clientOrder, Parental<R> parental) {
		parental.listeners.add(clientOrderListener);
		Order<F> exchangeOrder = repo.acquire();
		exchangeOrder.init(idgen.getClOrdID(), parental.getAvailableQty(), null);
		parental.addChild(exchangeOrder);
		exchangeOrder.listeners.add(exchangeOrderPublisher);
		exchangeOrder.listeners.add(exchangeOrderListener);
		repo.addOrder(exchangeOrder);
		exchangeOrders.put(parental, exchangeOrder);
		clientOrders.put(exchangeOrder, clientOrder);
	}

	private OrderListener<R, Parental<R>> clientOrderListener = new OrderListener<>() {
		@Override
		public void onCancelRequest(Parental<R> parental) {
			exchangeOrders.get(parental).requestCancel();
		}

		@Override
		public void onReplaceRequest(Parental<R> parental) {
			Order exchangeOrder = exchangeOrders.get(parental);
			Order clientOrder = clientOrders.get(exchangeOrder);
			exchangeOrder.replaceRequest.init(null, 0, clientOrder.replaceRequest.getQty(), clientOrder.replaceRequest.getFields());
		}
	};

	private OrderListener<F, Order<F>> exchangeOrderListener = new OrderListener<>() {
		@Override
		public void onFill(Order<F> exchangeOrder, long qty, double px) {
			clientOrders.get(exchangeOrder).fill(qty, px);
		}

		@Override
		public void onNonFillExecutionReport(Order<F> exchangeOrder, ExecType execType) {
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
		public void onCancelReject(Order<F> exchangeOrder, CxlRejReason rejectReason) {
			clientOrders.get(exchangeOrder).cancelRequest.reject();
		}

		public void onReplaceReject(Order exchangeOrder, CxlRejReason rejectReason) {
			clientOrders.get(exchangeOrder).replaceRequest.reject();
		}
	};
}
