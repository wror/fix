package com.bavelsoft.fix;

import java.util.Map;
import java.util.Collection;
import javax.inject.Inject;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ChildOrderRepo<O extends ChildOrder<F>, F> {
	private final OrderRepo<O, F> orderRepo;
	private final IdGenerator idgen;
	private final ChildByParentRepo childrenByParent;

	public ChildOrderRepo(OrderRepo<O, F> orderRepo, IdGenerator idgen, ChildByParentRepo childrenByParent) {
		this.orderRepo = orderRepo;
		this.idgen = idgen;
		this.childrenByParent = childrenByParent;
		orderRepo.requestRepo.invariants = childrenByParent;
	}

	public O requestNew(F fields, long orderQty, Order<?> parent) {
		O child = orderRepo.requestNew(fields, orderQty, idgen.getClOrdID());
		childrenByParent.add(parent, child);

		return child;
	}

	public void removeIfTerminal(O order) {
		orderRepo.removeIfTerminal(order);
		childrenByParent.removeIfTerminal(order);
	}

	public O get(long id) {
		return orderRepo.get(id);
	}

	public Collection<ChildOrder> get(Order parent) {
		return childrenByParent.get(parent);
	}

	public long getChildWorkingQty(Order parent) {
		return childrenByParent.getChildWorkingQty(parent);
	}
}

