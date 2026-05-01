package broke.fix.upstream;

import broke.fix.dto.CxlRejReason;
import broke.fix.dto.ExecRestatementReason;
import broke.fix.dto.OrdRejReason;

public class ReasonExceptions {
	public static class OrdRejException extends RuntimeException {
		public final OrdRejReason ordRejReason;

		public OrdRejException(OrdRejReason ordRejReason) {
			this.ordRejReason = ordRejReason;
		}
	}

	public static OrdRejReason ordRejReason(RuntimeException e) {
		if (e instanceof OrdRejException) {
			return ((OrdRejException)e).ordRejReason;
		} else {
			return OrdRejReason.Other;
		}
	}

	public static class ExecRestatementException extends RuntimeException {
		public final ExecRestatementReason execRestatementReason;

		public ExecRestatementException(ExecRestatementReason execRestatementReason) {
			this.execRestatementReason = execRestatementReason;
		}
	}

	public static ExecRestatementReason execRestatementReason(RuntimeException e) {
		if (e instanceof ExecRestatementException) {
			return ((ExecRestatementException)e).execRestatementReason;
		} else {
			return ExecRestatementReason.Other;
		}
	}

	public static class CxlRejException extends RuntimeException {
		public final CxlRejReason cxlRejReason;

		public CxlRejException(CxlRejReason cxlRejReason) {
			this.cxlRejReason = cxlRejReason;
		}
	}

	public static CxlRejReason cxlRejReason(RuntimeException e) {
		if (e instanceof CxlRejException) {
			return ((CxlRejException)e).cxlRejReason;
		} else {
			return CxlRejReason.Other;
		}
	}
}