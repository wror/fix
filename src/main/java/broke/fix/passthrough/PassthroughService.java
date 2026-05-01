package broke.fix.passthrough;

import broke.fix.CompositeOrder;
import broke.fix.Order;
import broke.fix.dto.CxlRejReason;
import broke.fix.dto.ExecType;
import broke.fix.misc.FixFields;
import broke.fix.misc.OrderListener;
import broke.fix.upstream.UpstreamRepository;

import javax.inject.Inject;

public class PassthroughService<F extends FixFields> {
	// private final UpstreamRepository<F> repo;
	// private final OrderListener exchangeOrderPublisher;

	// @Inject
	// public PassthroughService(UpstreamRepository exchangeOrderRepo, OrderListener exchangeOrderPublisher) {
	// 	this.repo = exchangeOrderRepo;
	// 	this.exchangeOrderPublisher = exchangeOrderPublisher;
	// }
	
	// /*
	//  * Callers need to pass in the unwrapped Order of the client order, because the lifecyle of its requests are tied to the exchange order's requests.
	//  */
	// public void slicePassthrough(Order clientOrder, F sliceFields) {
	// 	Order<F> exchangeOrder = repo.acquire().init(idgen.getClOrdID(), sliceFields, listenerPool.acquire().to(clientOrder), exchangeOrderPublisher);
	// 	repo.addOrder(exchangeOrder);
	// 	composite.addChild(exchangeOrder);
	// }

	// public void requestCancelOfPassthrough(CompositeOrder<?> composite) {
	// 	for (Order<?> child : composite.getChildren()) {
	// 		for (OrderListener listener : child.listeners()) {
	// 			if (listener instanceof PassthroughService<?>.PassthroughOrderListener) {
	// 				child.listeners().remove(listener);
	// 				child.requestCancel();
	// 				return;
	// 			}
	// 		}
	// 	}
	// 	//TODO indicate there is no passthrough slice?
	// }

	// class PassthroughOrderListener implements OrderListener<F, Order<F>> {
	// 	Order clientOrder;

	// 	OrderListener<F, Order<F>> to(Order clientOrder) {
	// 		this.clientOrder = clientOrder;
	// 		return this;
	// 	}

	// 	@Override
	// 	public void onExecutionReport(Order exchangeOrder, ExecType execType, long qty, double px) {
	// 		switch (execType) {
	// 			case New:
	// 				clientOrder.newRequest.accept(null);
	// 				break;
	// 			case Rejected:
	// 				clientOrder.newRequest.rejectRequest();
	// 				break;
	// 			case Trade:
	// 			case TradeCancel:
	// 				clientOrder.fill(qty, px);
	// 				break;
	// 			case Replaced:
	// 				clientOrder.replaceRequest.accept();
	// 				break;
	// 			case DoneForDay:
	// 				clientOrder.done();
	// 				break;
	// 			case Canceled:
	// 				clientOrder.cancel();
	// 				break;
	// 		}
	// 		if (!exchangeOrder.isWorking()) {
	// 			exchangeOrder.listeners.remove(this);
	// 			listenerPool.release(this);
	// 		}
	// 	}

	// 	@Override
	// 	public void onCancelReject(Order exchangeOrder, CxlRejReason rejectReason) {
	// 		clientOrder.cancelRequest.rejectRequest();
	// 	}

	// 	@Override
	// 	public void onReplaceReject(Order exchangeOrder, CxlRejReason rejectReason) {
	// 		clientOrder.replaceRequest.rejectRequest();
	// 	}
	// }
}
