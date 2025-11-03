package broke.fix.misc;

import broke.fix.OrderComponent;
import broke.fix.Order;
import broke.fix.OrderComposite;
import broke.fix.dto.ExecType;

import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FixRepository<F, H extends OrderComponent<F, H>> {
	private final static Logger log = LogManager.getLogger();
	private final SimplePool<F> fieldsPool;
	private final SimplePool<H> orderPool;
	private final Map<CharSequence, H> orderByClOrdID;
	private final Map<CharSequence, H> orderByOrderID;
	private final OrderRepository<F, H> sharedOrderRepo;

	public FixRepository(SimplePool<F> fieldsPool, SimplePool<H> orderPool, Map<CharSequence, H> orderByClOrdID, Map<CharSequence, H> orderByOrderID, OrderRepository<F, H> sharedOrderRepo) {
		this.fieldsPool = fieldsPool;
		this.orderPool = orderPool;
		this.orderByClOrdID = orderByClOrdID;
		this.orderByOrderID = orderByOrderID;
		this.sharedOrderRepo = sharedOrderRepo;
	}

	public H acquire() {
		return orderPool.acquire();
	}

	public void release(H order) {
		if (order == null) {
			return;
		}
		orderPool.release(order);
		fieldsPool.release((F)order.view().getFields()); //TODO why is this cast nesc?
	}

	public void addOrder(H order) {
		order.listeners().add(keyUpdatingListener);
		orderByClOrdID.put(order.view().getClOrdID(), order);
		sharedOrderRepo.addOrder(order);
	}

	public void removeClOrdID(CharSequence origClOrdID) {
		orderByClOrdID.remove(origClOrdID);
	}

	public void putClOrdID(CharSequence clOrdID, H order) {
		orderByClOrdID.put(clOrdID, order);
	}
	
	OrderListener<F, H> keyUpdatingListener = new OrderListener<F, H>() {
		@Override
		public void onNewRequest(H order) {
			orderByOrderID.put(order.view().getOrderID(), order);
		}

		@Override
		public void onExecutionReport(H order, ExecType execType, long qty, double px) {
			//TODO give flexibility about whether to support amending up filled orders
			if (order.getWorkingQty() <= 0) {
				log.info("Order {} recycled", order.view().getOrderID());
				removeOrder(order);
			}
		}
	};

	public void removeOrder(H order) {
		if (order.view().isRoot() ) {
			removeClOrdID(order.view().getOptimisticClOrdID());
		} else if (order.view().getOrderID().isEmpty()) { //DownstreamHandler might've called removeClOrdID already
			removeClOrdID(order.view().getClOrdID());
		}
		orderByOrderID.remove(order.view().getOrderID());
		sharedOrderRepo.removeOrder(order);
		release(order);
	}

	public H getOrder(CharSequence orderID, CharSequence clOrdID) {
		if (orderID != null) { //Will be null for the first message from downstream for each order
			H order = orderByOrderID.get(orderID);
			if (order != null) {
				return order;
			}
		}
		return getOrderByClOrdIdOrLog(orderID, clOrdID);
	}

	private H getOrderByClOrdIdOrLog(Object orderID, CharSequence clOrdID) {
		H order = orderByClOrdID.get(clOrdID);
		if (order != null) {
			return order;
		}
		log.warn("Couldn't find order {}/{}", orderID, clOrdID);
		return null;
	}
}
