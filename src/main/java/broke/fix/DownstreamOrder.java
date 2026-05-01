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
import broke.fix.request.CancelRequest;
import broke.fix.request.NewRequest;
import broke.fix.request.ReplaceRequest;

public final class DownstreamOrder<F extends FixFields> extends Order<F> {
	private final static Logger log = LogManager.getLogger();
	private long potentialOrderQty;
	private long requestCounter;
	CharSequence downstreamOrderID;

	public DownstreamOrder(IncomingContext context, F fields, OrderListener<DownstreamOrder<F>>... listeners) {
		super(context, fields, new ArrayList<>(Arrays.asList(listeners)));
	}

	@Override
	protected void onReject(Request<F> request) {
		if (request instanceof ReplaceRequest) {
			updatePotentialOrderQty();
		}

		this.clOrdID = request.getOrigClOrdID();
	}

	@Override
	public NewRequest<F> requestNew() {
		return new NewRequest<>(nextClOrdID(), this);
	}

	@Override
	public ReplaceRequest<F> requestReplace(F newFields) {
		updatePotentialOrderQty();
		return new ReplaceRequest<F>(clOrdID, nextClOrdID(), this, newFields);
	}

	@Override
	public CancelRequest<F> requestCancel() {
		return new CancelRequest<>(clOrdID, nextClOrdID(), this);
	}

	private CharSequence nextClOrdID() {
		return clOrdID = getInternalOrderID()+"."+requestCounter++;
	}

	@Override
	protected void replace(F fields) {
		super.replace(fields);
		updatePotentialOrderQty();
	}

	private void updatePotentialOrderQty() {
		long newPotentialOrderQty = 0;
		for (Request<F> request : requests) {
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

	@Override
	public void forceCancel() {
		cancel();
	}

	public void rejectRequest(CharSequence clOrdID, CxlRejReason reason) {
		for (Request<?> request: requests) {
			if (request.getClOrdID().equals(clOrdID)) {
				request.reject(reason);
			}
		}
	}

	public void acceptReplace(CharSequence clOrdID, long orderQty) {
		for (Request<?> request: requests) {
			if (request instanceof ReplaceRequest && request.getClOrdID().equals(clOrdID)) {
				request.accept();
				return;
			}
		}
		log.warn("No clordid matching the Replaced executionreport, matching by orderQty");
		for (Request<?> request: requests) {
			if (request instanceof ReplaceRequest && request.getQty() == orderQty) {
				request.accept();
				return;
			}
		}
		log.error("Nothing matching the Replaced executionreport!");
	}
}
