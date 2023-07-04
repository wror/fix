package com.bavelsoft.fix;

import java.util.Map;
import java.util.Collection;
import javax.inject.Inject;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Collections.emptySet;

public class ChildByParentRepo implements Consumer<Order> {
	private final Map<Order, Collection<ChildOrder>> map;

	public ChildByParentRepo(Map<Order, Collection<ChildOrder>> map, OrderRepo parentOrderRepo, Supplier<Collection> collectionFactory) {
		this.map = map;

		for (Object order : parentOrderRepo.all()) {
			map.put((Order)order, collectionFactory.get());
			((Order)order).invariants = this;
		}
	}

	public void add(Order parent, ChildOrder child) {
		child.init(parent);
		map.get(parent).add(child);
		accept(parent);
	}

	public Collection<ChildOrder> get(Order parent) {
		Collection<ChildOrder> collection = map.get(parent);
		return collection;
	}

	public void removeIfTerminal(ChildOrder order) {
		if (order.getLeavesQty() == 0) {
			map.get(order.getParent()).remove(order);
		}
	}

	public long getChildWorkingQty(Order parent) {
		long childQty = 0;
		Collection<ChildOrder> children = get(parent);
		for (ChildOrder child : children) {
			childQty += child.getWorkingQty();
		}
		return childQty;
	}

	public void accept(Order order) {
		if (order instanceof ChildOrder) {
			order = ((ChildOrder)order).getParent();
		}
		assert getChildWorkingQty(order) <= order.getLeavesQty();
	}
}
