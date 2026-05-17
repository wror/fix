package broke.fix.misc;

import static java.util.List.of;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Predicate;

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
	public static ReasonTuple other =
		new ReasonTuple(Reason.Other,				CxlRejReason.Other,				OrdRejReason.Other);
	private static Collection<ReasonTuple> reasons = of(
		new ReasonTuple(Reason.TooLate,	CxlRejReason.TooLateToCancel,	OrdRejReason.TooLateToEnter),
		new ReasonTuple(Reason.DuplicateClOrdID,	CxlRejReason.DuplicateClOrdID,	OrdRejReason.DuplicateClOrdID),
		new ReasonTuple(Reason.UnknownOrder,	CxlRejReason.UnknownOrder,	null),
		other
	);

	public static Reason reason(OrdRejReason ordRej) {
		return get(r->r.ordRej==ordRej, r->r.reason);
	}

	public static Reason reason(CxlRejReason cxlRej) {
		return get(r->r.cxlRej==cxlRej, r->r.reason);
	}

	public static OrdRejReason ordRejReason(Exception e) {
		return ordRejReason(reason(e));
	}

	public static OrdRejReason ordRejReason(Reason reason) {
		return get(r->r.reason==reason, r->r.ordRej);
	}

	public static CxlRejReason cxlRejReason(Order<?> order, Exception e) {
		return order == null ? CxlRejReason.UnknownOrder : cxlRejReason(reason(e));
	}

	public static CxlRejReason cxlRejReason(Reason reason) {
		return get(r->r.reason==reason, r->r.cxlRej);
	}

	private static <R> R get(Predicate<ReasonTuple> filter, Function<ReasonTuple, R> map) {
		return map.apply(reasons.stream().filter(filter).findFirst().orElse(other));
	}

	public static Reason reason(Exception e) {
		if (e instanceof FixException) {
			return ((FixException)e).reason;
		} else {
			return Reason.Other;
		}
	}
}
