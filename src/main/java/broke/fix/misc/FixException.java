package broke.fix.misc;

import static java.util.List.of;

import java.util.Collection;

import broke.fix.Order;
import broke.fix.dto.CxlRejReason;
import broke.fix.dto.OrdRejReason;

public class FixException extends Exception {
	public enum Reason { TooLate, UnknownOrder, DuplicateClOrdID, Other }
	public final Reason reason;
	public FixException(Reason reason) {
		this.reason = reason;
	}

	record ReasonTuple(Reason reason, CxlRejReason cxlRej, OrdRejReason ordRej) {}
	private static Collection<ReasonTuple> reasons = of(
		new ReasonTuple(Reason.TooLate,          CxlRejReason.TooLateToCancel,  OrdRejReason.TooLateToEnter),
		new ReasonTuple(Reason.DuplicateClOrdID, CxlRejReason.DuplicateClOrdID, OrdRejReason.DuplicateClOrdID),
		new ReasonTuple(Reason.Other,            CxlRejReason.Other,            OrdRejReason.Other),
		new ReasonTuple(Reason.UnknownOrder,     CxlRejReason.UnknownOrder,     null)
	);

	public static Reason reason(OrdRejReason ordRej) {
		return reasons.stream().filter(r->r.ordRej==ordRej).findFirst().map(r->r.reason).orElse(Reason.Other);
	}

	public static Reason reason(CxlRejReason cxlRej) {
		return reasons.stream().filter(r->r.cxlRej==cxlRej).findFirst().map(r->r.reason).orElse(Reason.Other);
	}

	public static OrdRejReason ordRejReason(Exception e) {
		return ordRejReason(reason(e));
	}

	public static OrdRejReason ordRejReason(Reason reason) {
		return reasons.stream().filter(r->r.reason==reason).findFirst().map(r->r.ordRej).orElse(OrdRejReason.Other);
	}

	public static CxlRejReason cxlRejReason(Order<?> order, Exception e) {
		return order == null ? CxlRejReason.UnknownOrder : cxlRejReason(reason(e));
	}

	public static CxlRejReason cxlRejReason(Reason reason) {
		return reasons.stream().filter(r->r.reason==reason).findFirst().map(r->r.cxlRej).orElse(CxlRejReason.Other);
	}

	public static Reason reason(Exception e) {
		if (e instanceof FixException) {
			return ((FixException)e).reason;
		} else {
			return Reason.Other;
		}
	}
}
