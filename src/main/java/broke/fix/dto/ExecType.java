package broke.fix.dto;

import broke.fix.Request;
import broke.fix.request.NewRequest;

public enum ExecType {
	New, DoneForDay, Canceled, Replaced, PendingCancel, Rejected, PendingNew, Restated, PendingReplace, Trade, TradeCorrect, TradeCancel;

	public boolean isRejectFor(Request request) {
		return (this == Rejected && request instanceof NewRequest);
	}
}
