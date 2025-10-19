package broke.fix.test;

import broke.fix.dto.CxlRejReason;
import broke.fix.dto.CxlRejResponseTo;
import broke.fix.dto.ExecInst;
import broke.fix.dto.ExecType;
import broke.fix.dto.OrdStatus;

record Message(ExecType execType, CxlRejResponseTo rejType, OrdStatus ordStatus, long cumQty, long leavesQty, long lastShares, CharSequence clOrdID, CharSequence origClOrdID) {
	Message(CxlRejResponseTo rejType, OrdStatus ordStatus, CharSequence clOrdID, CharSequence origClOrdID) {
		this(null, rejType, ordStatus, -1, -1, -1, clOrdID, origClOrdID);
	}

	Message(ExecType execType, OrdStatus ordStatus, long cumQty, long leavesQty, long lastShares, CharSequence clOrdID, CharSequence origClOrdID) {
		this(execType, null, ordStatus, cumQty, leavesQty, lastShares, clOrdID, origClOrdID);
	}
}
