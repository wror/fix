package com.bavelsoft.fix;

import java.util.Map;
import java.util.Collection;
import javax.inject.Inject;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ChildOrderService {
	private final IdGenerator idgen;
	private final RequestRepo requestRepo;
	private final ChildOrderRepo childOrderRepo;
	private final ParentCanceller parentCanceller = new ParentCanceller();

	public ChildOrderService(IdGenerator idgen, RequestRepo requestRepo, ChildOrderRepo childOrderRepo) {
		this.idgen = idgen;
		this.requestRepo = requestRepo;
		this.childOrderRepo = childOrderRepo;
	}

	class ParentCanceller {
		public void accept(ChildOrder order) {
			Order parent = order.getParent();
			if (childOrderRepo.getChildWorkingQty(parent) == 0) {
				parent.cancel();
			}
		}
	}

	public void cancelFamily(Order<?> parent) {
		Collection<ChildOrder> children = childOrderRepo.get(parent);
		if (children.isEmpty()) {
			parent.cancel();
			return;
		}
		for (ChildOrder<?> child : children) {
			child.parentCanceller = parentCanceller;
			requestRepo.requestCancel(child, idgen.getClOrdID());
		}
	}

	public void forceCancelFamily(Order parent) {
		Collection<ChildOrder> children = childOrderRepo.get(parent);
		for (ChildOrder child : children) {
			child.cancel();
		}
		parent.cancel();
	}
}

