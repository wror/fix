package broke.fix;

import static java.lang.Long.max;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import broke.fix.dto.CxlRejReason;
import broke.fix.dto.ExecType;
import broke.fix.dto.OrdStatus;
import broke.fix.misc.FixFields;
import broke.fix.misc.IncomingContext;
import broke.fix.misc.OrderListener;
import broke.fix.request.ReplaceRequest;

public final class SendableOrder<F extends FixFields> extends Order<F> {
	private final static Logger log = LogManager.getLogger();
	private long potentialOrderQty;
	private CharSequence downstreamOrderID;

	public SendableOrder(IncomingContext context, F fields, OrderListener<SendableOrder<F>>... listeners) {
		super(context, fields, new ArrayList<>(Arrays.asList(listeners)));
	}

	@Override
	public void onRequestChange(Request<?, F> request) {
		super.onRequestChange(request);
		if (request instanceof ReplaceRequest) {
			updatePotentialOrderQty();
		}
		if (request.getStatus() == Request.Status.Rejected) {
			setClOrdID(request.getOrigClOrdID());
		}
	}

	private void updatePotentialOrderQty() {
		long newPotentialOrderQty = getFields().getOrderQty();
		for (Request<?, F> request : requests()) {
			newPotentialOrderQty = max(newPotentialOrderQty, request.getQty());
		}
		addWorkingQtyChange(newPotentialOrderQty - potentialOrderQty);
		potentialOrderQty = newPotentialOrderQty;
	}

	@Override
	public long getWorkingQty() {
		if (!isWorking()) {
			return 0;
		}
		return max(0, potentialOrderQty - getCumQty());
	}

	public void cancel() {
		super.cancel();
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

	public void rejectRequest(CharSequence clOrdID, CxlRejReason reason) {
		for (Request<?, ?> request: requests()) {
			if (request.getClOrdID().equals(clOrdID)) {
				request.reject(reason);
			}
		}
	}

	public void acceptReplace(CharSequence clOrdID) {
		for (Request<?, ?> request: requests()) {
			if (request instanceof ReplaceRequest && request.getClOrdID().equals(clOrdID)) {
				request.accept();
				return;
			}
		}
		log.error("Nothing matching the Replaced execution report!");
	}
}
