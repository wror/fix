package broke.fix.misc;

import java.util.Queue;
import java.util.function.Supplier;

public class SimplePool<T> {
	private final Queue<T> queue;
	private final Supplier<T> constructor;

	//auditing to make sure there can be no dangling references to released orders:
	//search for Field Declaration, Type Parameter, Usage in extends/implements clauses of the class and its interfaces 
	//make sure all of those (except this class) are updated before we call release, or check them in turn
	//

	public SimplePool(Queue<T> queue, Supplier<T> constructor, int size) {
		this.queue = queue;
		this.constructor = constructor;
		for (int i=0; i<size; i++) {
			queue.add(constructor.get());
		}
	}

	public T acquire() {
		return queue.remove();
	}

	public void release(T object) {
		if (object != null) {
			queue.add(object);
		}
	}
}
