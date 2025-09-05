package com.bavelsoft.fix;

import java.util.Map;
import java.util.Collection;
import javax.inject.Inject;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Collections.emptySet;

/*
   ChildOrderRepo supports two methods in addition to those of a regular OrderRepo:
	getChildren(Order parent)
	getChildrenWorkingQty(Order parent)
 */
public class ChildOrderRepo<O extends ChildOrder<F>, F> implements Consumer<Order> {
	private final OrderRepo<O, F> orderRepo;
	private final IdGenerator idgen;
	private final Supplier<Collection> collectionFactory;
	private final Map<Order, Collection<ChildOrder>> childrenByParent;

	public ChildOrderRepo(Map<Long, O> map, SimplePool<O> pool, IdGenerator idgen, RequestRepo requestRepo,
			Map<Order, Collection<ChildOrder>> childrenByParent, Supplier<Collection> collectionFactory) {
		this.orderRepo = new OrderRepo(map, pool, idgen, requestRepo);
		this.idgen = idgen;
		this.childrenByParent = childrenByParent;
		this.collectionFactory = collectionFactory;
		orderRepo.requestRepo.invariants = this;
	}

	public O requestNew(F fields, long orderQty, Order<?> parent) {
		O child = orderRepo.requestNew(fields, orderQty, idgen.getClOrdID());
		child.init(parent);
		childrenByParent.putIfAbsent(parent, collectionFactory.get()); //TODO computeIfAbsent for garbage reduction
		childrenByParent.get(parent).add(child);
		accept(parent);

		return child;
	}

	public void remove(O order) {
		orderRepo.remove(order);
		childrenByParent.get(order.getParent()).remove(order);
	}

	public O get(long id) {
		return orderRepo.get(id);
	}

	public Collection<ChildOrder> getChildren(Order parent) {
		return childrenByParent.get(parent);
	}

	public long getChildrenWorkingQty(Order parent) {
		long childQty = 0;
		for (ChildOrder child : getChildren(parent)) {
			childQty += child.getWorkingQty();
		}
		return childQty;
	}

	public void accept(Order order) {
		if (order instanceof ChildOrder) {
			order = ((ChildOrder)order).getParent();
		}
		assert getChildrenWorkingQty(order) <= order.getLeavesQty();
	}
}
