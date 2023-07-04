package com.bavelsoft.fix;

import java.util.List;
import java.util.function.Supplier;

public class SimplePool<T> {
	final List<T> pool;
	private final Supplier<T> constructor;

	//search for Field Declaration, Type Parameter, Usage in extends/implements clauses of the class and its interfaces 
	//make sure all of those (except this class) are updated before we call release, or check them in turn
	//

	public SimplePool(List<T> poolList, Supplier<T> constructor, int size) {
		this.pool = poolList;
		this.constructor = constructor;
		for (int i=0; i<size; i++) {
			pool.add(constructor.get());
		}
	}

	public T acquire() {
		return pool.remove(pool.size()-1);
	}

	public void release(T order) {
		pool.add(order);
	}
}
