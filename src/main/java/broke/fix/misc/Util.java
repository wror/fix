package broke.fix.misc;

import broke.fix.dto.ExecRestatementReason;
import broke.fix.dto.OrdRejReason;

public class Util {
	public static final OrdRejReason ordRejReason(Object reason) {
		if (reason instanceof OrdRejReason) {
			return (OrdRejReason)reason;
		} else {
			return OrdRejReason.Other;
		}
	}

	public static final ExecRestatementReason execRestatementReason(Object reason) {
		if (reason instanceof ExecRestatementReason) {
			return (ExecRestatementReason)reason;
		} else {
			return ExecRestatementReason.Other;
		}
	}
}
