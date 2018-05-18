package data;

import java.util.ArrayDeque;

/**
 * Keep a buffer of some objects. Oldest gets thrown out to keep the size 
 * at/below a maximum. Using an iterator, you can go through it LIFO-style.
 * @author matt
 *
 * @param <T>
 */
public class LIFOFixedSize<T>  extends ArrayDeque<T> {

	private static final long serialVersionUID = 1L;

	private final int maxSize;

	public LIFOFixedSize(int maxSize) {
		this.maxSize = maxSize;
	}

	@Override
	public void push(T elt) {
		super.push(elt);
		while (this.size() > this.maxSize) {
			pollLast();
		}
	}
}

