package wheel.reactive.maps.impl;

import wheel.reactive.maps.MapValueChange;

public class MapElementReplaced<K,V> implements MapValueChange<K,V> {

	private final K _key;
	private final V _value;

	public MapElementReplaced(K key, V value) {
		_key = key;
		_value = value;
	}

	@Override
	public void accept(Visitor<K,V> visitor) {
		visitor.entryReplaced(_key,_value);
	}
	
	@Override
	public String toString() {
		return "Map element replaced at " + _key;
	}

}
