package broke.fix.misc;

import java.util.Map;

import javax.annotation.Nullable;

public class ExecutionRepository {
	private final Map<CharSequence, Execution> executionsByExecID;

	public ExecutionRepository(Map<CharSequence, Execution> executionsByExecID) {
		this.executionsByExecID = executionsByExecID;
	}

	public void addExecution(CharSequence execID, Execution execution) {
		executionsByExecID.put(execID, execution);
	}

	public @Nullable Execution getExecution(CharSequence execID) {
		return executionsByExecID.get(execID);
	}
}
