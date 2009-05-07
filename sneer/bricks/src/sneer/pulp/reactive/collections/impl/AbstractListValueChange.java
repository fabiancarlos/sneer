package sneer.pulp.reactive.collections.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import sneer.pulp.reactive.collections.CollectionChange;
import sneer.pulp.reactive.collections.ListChange;

abstract class AbstractListValueChange<T>  implements CollectionChange<T>, ListChange<T> {

	protected final int _index;
	protected final T _element;
	
	AbstractListValueChange(int index, T element) {
		_index = index;
		_element = element;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " index:" + _index + " element:" + _element; 
	}

	protected ArrayList<T> newColection(T element) {
		ArrayList<T> result = new ArrayList<T>();
		result.add(element);
		return result;
	}

	@Override
	public Collection<T> elementsAdded() {
		return Collections.EMPTY_LIST;
	}

	@Override
	public Collection<T> elementsRemoved() {
		return newColection(_element);
	}
}