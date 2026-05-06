package broke.fix;

import static java.lang.Long.max;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import broke.fix.dto.ExecType;
import broke.fix.dto.OrdStatus;
import broke.fix.misc.FixException.Reason;
import broke.fix.misc.FixFields;
import broke.fix.misc.IncomingContext;
import broke.fix.misc.OrderListener;
import broke.fix.request.ReplaceRequest;

//Could be sent downstream, and is properly careful about pending changes to orderQty
public final class SendableOrder<F extends FixFields> extends Order<F> {
	private final static Logger log = LogManager.getLogger();
	private long potentialOrderQty;
	private CharSequence downstreamOrderID;

	@SafeVarargs 
	public SendableOrder(IncomingContext context, F fields, OrderListener<SendableOrder<F>, F>... listeners) {
		super(context, fields, safeAsList(listeners));
	}

	@Override
	public void onRequestChange(Request request, Request.Status requestStatus) {
		super.onRequestChange(request, requestStatus);
		if (request instanceof ReplaceRequest) {
			updatePotentialOrderQty();
		}
		if (requestStatus == Request.Status.Pending) {
			setClOrdID(request.clOrdID); // optimistic per https://www.fixtrading.org/online-specification/business-area-trade/#:~:text=The%20order%20sender%20should%20chain,this.%29
		} else if (requestStatus == Request.Status.Rejected) {
			setClOrdID(request.origClOrdID);
		}
	}

	private void updatePotentialOrderQty() {
		long newPotentialOrderQty = getFields().getOrderQty();
		for (Request request : requests()) {
			newPotentialOrderQty = max(newPotentialOrderQty, request.getRequestedOrderQty());
		}
		addWorkingQtyChange(getCategory(), newPotentialOrderQty - potentialOrderQty);
		potentialOrderQty = newPotentialOrderQty;
	}

	@Override
	public long getWorkingQty() {
		if (!isOpen()) {
			return 0;
		}
		return max(0, potentialOrderQty - getCumQty());
	}

	public final void fill(final long qty, final double px) {
		fill(getCategory(), qty, px);
	}

	public void cancel(CharSequence text) {
		super.cancel(text);
	}

	public void done() {
		terminate(OrdStatus.DoneForDay, ExecType.DoneForDay, null);
	}

	public CharSequence getDownstreamOrderID() {
		return downstreamOrderID;
	}

	public void setDownstreamOrderID(CharSequence downstreamOrderID) {
		this.downstreamOrderID = downstreamOrderID;
	}

	public void rejectRequest(CharSequence clOrdID, Reason reason) {
		for (Request request: requests()) {
			if (request.clOrdID.equals(clOrdID)) {
				request.reject(reason);
			}
		}
		log.error("Nothing matching the OrderCancelReject!");
	}

	public void acceptReplace(CharSequence clOrdID) {
		for (Request request: requests()) {
			if (request instanceof ReplaceRequest && request.clOrdID.equals(clOrdID)) {
				request.accept();
				return;
			}
		}
		log.error("Nothing matching the Replaced ExceptionReport!");
	}
}
