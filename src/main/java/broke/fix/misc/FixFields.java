package broke.fix.misc;

public interface FixFields {
	long getOrderQty();
	default double getPrice() { return 0; }
	default long getOrigOrdModTime() { return 0; }

	ExecInst[] emptyExecInst = new ExecInst[0];
	default ExecInst[] getExecInst() { return emptyExecInst; }
	default boolean hasExecInst(ExecInst i) {
		//implementers can provide an optimized implementation
		for (ExecInst fi : getExecInst()) {
			if (fi == i) {
				return true;
			}
		}
		return false;
	}
}
