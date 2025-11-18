package broke.fix.misc;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.annotation.Nullable;

public class ExecutionRepository {
	private final static Logger log = LogManager.getLogger();
	private final SimplePool<Execution> pool;
	private final Map<CharSequence, Execution> executionsByExecID;

	public ExecutionRepository(SimplePool<Execution> pool, Map<CharSequence, Execution> executionsByExecID) {
		this.pool = pool;
		this.executionsByExecID = executionsByExecID;
	}

	public Execution acquire() {
		return pool.acquire();
	}

	public void addExecution(CharSequence execID, Execution execution) {
		executionsByExecID.put(execID, execution);
	}

	//when could this ever be called?
	public void removeExecution(CharSequence execID) {
		Execution execution = executionsByExecID.remove(execID);
		pool.release(execution);
	}

	public @Nullable Execution getExecution(CharSequence execID) {
		return executionsByExecID.get(execID);
	}
}
