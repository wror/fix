package com.bavelsoft.fix;

public class ChildOrder<F> extends Order<F> {
	ChildOrderService.ParentCanceller parentCanceller;
	private Order<?> parent;

	//needs to be called in addition to super.init()
	void init(Order<?> parent) {
		this.parentCanceller = null;
		this.parent = parent;
	}

	public Order<?> getParent() {
		return parent;
	}

	@Override
	public void fill(long qty, double px) {
		super.fill(qty, px);
		parent.fill(qty, px);
	}


	@Override
	protected void terminate(OrdStatus status) {
		super.terminate(status);
		if (parentCanceller != null) {
			parentCanceller.accept(this);
		}
	}

	public long getWorkingQty() {
		return replaceRequest.getWorkingQty();
	}

	public CharSequence getClOrdID() {
		if (cancelRequest.isAccepted()) {
			return cancelRequest.getClOrdID();
		} else if (replaceRequest.isAccepted()) {
			return replaceRequest.getClOrdID();
		} else {
			return newRequest.getClOrdID();
		}
	}

}
