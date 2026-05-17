package broke.fix.dto;

import broke.fix.Request;
import broke.fix.request.CancelRequest;
import broke.fix.request.ReplaceRequest;

public enum CxlRejResponseTo {
	Cancel, Replace;

	public boolean isRejectFor(Request request) {
		return (this == Cancel && request instanceof CancelRequest) || (this == Replace && request instanceof ReplaceRequest);
	}
}
