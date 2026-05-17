package broke.fix.misc;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;

import static broke.fix.misc.FixException.reason;

import broke.fix.CompositeOrder;
import broke.fix.Order;
import broke.fix.Request;
import broke.fix.SendableOrder;
import broke.fix.dto.CxlRejReason;
import broke.fix.dto.CxlRejResponseTo;
import broke.fix.dto.ExecType;
import broke.fix.dto.OrdRejReason;
import broke.fix.dto.OrdStatus;
import broke.fix.request.CancelRequest;
import broke.fix.request.ReplaceRequest;
import broke.fix.upstream.UpstreamHandler;
import javax.inject.Inject;

public class UpstreamWithPassthroughHandler<F extends FixFields> {
	private final static Logger log = LogManager.getLogger();
	private UpstreamHandler<F> simpleHandler;

	@Inject
	public UpstreamWithPassthroughHandler(UpstreamHandler<F> simpleHandler) {
		this.simpleHandler = simpleHandler;
	}

	public void handleNewRequest(long transactTime, CharSequence instrumentID, CharSequence clOrdID, F fields) {
		simpleHandler.handleNewRequest(transactTime, instrumentID, clOrdID, fields);
		CompositeOrder<F> order = simpleHandler.repo.get(clOrdID);
		order.addChild(new SendableOrder<F>(simpleHandler.incoming, fields, downstreamListener));
	}

	private final OrderListener<SendableOrder<F>, F> downstreamListener = new OrderListener<>() {
		@Override
		public void onCancelOrReplaceReject(SendableOrder<F> order, CharSequence clOrdID, CxlRejResponseTo responseTo, @Nullable CxlRejReason rejectReason) {
			for (Request request : order.getParent().pendingRequsts()) {
				if (responseTo.isRejectFor(request)) {
					request.reject(reason(rejectReason));
					return;
				}
			}
			log.error("Unknnown pass through action");
		}

		@Override
		public void onOtherExecutionReport(SendableOrder<F> order, ExecType execType, @Nullable OrdRejReason ordRejReason, @Nullable CharSequence text) {
			CompositeOrder<F> parent = order.getParent();
			for (Request request : parent.pendingRequsts()) {
				if (execType.isRejectFor(request)) {
					request.reject(reason(ordRejReason));
					return;
				}
				if (request.getExecType() == execType) {
					request.accept();
					return;
				}
			}
			if (order.isClosed()) {
				parent.terminate(order.getOrdStatus(), execType, text);
				return;
			}
			log.error("Unknnown pass through action");
		}
	};

	public void handleReplaceRequest(long transactTime, CharSequence instrumentID, CharSequence clOrdID, CharSequence origClOrdID, F fields) {
		SendableOrder<F> child = getChild(clOrdID);
		if (child != null) {
			new ReplaceRequest<F>(getChild(clOrdID), fields);
		}
	}

	public void handleCancelRequest(long transactTime, CharSequence instrumentID, CharSequence clOrdID, CharSequence origClOrdID) {
		SendableOrder<F> child = getChild(clOrdID);
		if (child != null) {
			new CancelRequest(child);
		}
	}

	private SendableOrder<F> getChild(CharSequence clOrdID) {
		CompositeOrder<F> order = simpleHandler.repo.get(clOrdID);
		Order<F> child = order.getChildren().iterator().next();
		if (child.getOrdStatus() == OrdStatus.PendingCancel) { //request already propagated
			return null;
		}
		return (SendableOrder<F>)child;
	}
}
