package broke.fix.service;

import broke.fix.Order;
import broke.fix.OrderComposite;
import broke.fix.dto.CxlRejReason;
import broke.fix.dto.ExecType;
import broke.fix.misc.FixFields;
import broke.fix.misc.IdGenerator;
import broke.fix.misc.OrderListener;
import broke.fix.misc.FixRepository;
import broke.fix.misc.SimplePool;

import javax.inject.Inject;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.HashMap;

public class PassthroughFactory<F extends FixFields> {
	private final IdGenerator idgen;
	private final SimplePool<PassthroughOrderListener> listenerPool;
	private final FixRepository<F, Order<F>> repo;
	private final OrderListener exchangeOrderPublisher;

	@Inject
	public PassthroughFactory(IdGenerator idgen, FixRepository exchangeOrderRepo, SimplePool listenerPool, OrderListener exchangeOrderPublisher) {
		this.idgen = idgen;
		this.listenerPool = listenerPool;
		this.repo = exchangeOrderRepo;
		this.exchangeOrderPublisher = exchangeOrderPublisher;
	}

	public void slicePassthrough(Order clientOrder, OrderComposite composite, F sliceFields) {
		Order<F> exchangeOrder = repo.acquire().init(idgen.getClOrdID(), sliceFields, listenerPool.acquire().to(clientOrder), exchangeOrderPublisher);
		repo.addOrder(exchangeOrder);
		composite.addChild(exchangeOrder);
	}

	class PassthroughOrderListener implements OrderListener<F, Order<F>> {
		Order clientOrder;

		OrderListener<F, Order<F>> to(Order clientOrder) {
			this.clientOrder = clientOrder;
			return this;
		}

		@Override
		public void onExecutionReport(Order exchangeOrder, ExecType execType, long qty, double px) {
			switch (execType) {
				case New:
					clientOrder.newRequest.accept(null);
					break;
				case Rejected:
					clientOrder.newRequest.reject();
					break;
				case Trade:
					clientOrder.fill(qty, px);
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
			if (!exchangeOrder.view().isWorking()) {
				exchangeOrder.listeners.remove(this);
				listenerPool.release(this);
			}
		}

		@Override
		public void onCancelReject(Order exchangeOrder, CxlRejReason rejectReason) {
			clientOrder.cancelRequest.reject();
		}

		@Override
		public void onReplaceReject(Order exchangeOrder, CxlRejReason rejectReason) {
			clientOrder.replaceRequest.reject();
		}
	}
}
