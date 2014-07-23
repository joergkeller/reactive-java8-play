package service.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Scheduler<T> implements Supplier<T> {
	
	private final List<T> pool;
	private int next = 0;

	public Scheduler(int poolSize, java.util.function.Supplier<T> supplier) {
		pool = new ArrayList<T>(poolSize);
		for (int i = 0; i < poolSize; i++) {
			pool.add(supplier.get());
		}
	}

	@Override
	public synchronized T get() {
		try {
			return pool.get(next);
		} finally {
			next = (next + 1) % pool.size();
		}
	}

	public void forEach(Consumer<T> consumer) {
		for (T item : pool) {
			consumer.accept(item);
		}
	}

}
