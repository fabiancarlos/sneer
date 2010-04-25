package sneer.bricks.hardware.io.prevalence.map.impl;

import static sneer.foundation.environments.Environments.my;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import sneer.bricks.hardware.io.prevalence.flag.PrevalenceFlag;
import sneer.bricks.hardware.io.prevalence.map.ExportMap;

class ExportMapImpl implements ExportMap {
	
	private Map<Object, Long> _idsByObject = new ConcurrentHashMap<Object, Long>();
	private Map<Long, Object> _objectsById = new ConcurrentHashMap<Long, Object>();
	private long _nextId;

	
	@Override
	public long idByObject(Object object) {
		Long id = _idsByObject.get(object);
		if (id == null)
			throw new IllegalStateException("No id for '" + object + "'.");
		return id;
	}

	
	@Override
	public <T> T register(T object) {
		checkInsidePrevalence(object);
		
		if (_idsByObject.containsKey(object))
			throw new IllegalStateException();
		
		long id = nextId();
		_idsByObject.put(object, id);
		_objectsById.put(id, object);
		return object;
	}

	
	private <T> void checkInsidePrevalence(T object) {
		if (!my(PrevalenceFlag.class).isInsidePrevalence())
			throw new IllegalStateException("Trying to register object '" + object + "' outside prevalent environment.");
	}

	
	private long nextId() {
		return _nextId++;
	}

	
	@Override
	public Object objectById(long id) {
		return _objectsById.get(id);
	}

	
	@Override
	public boolean isRegistered(Object object) {
		return _idsByObject.containsKey(object);
	}
	
}