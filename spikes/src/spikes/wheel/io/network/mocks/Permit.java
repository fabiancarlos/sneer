//Copyright (C) 2004 Klaus Wuestefeld
//This is free software. It is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the license distributed along with this file for more details.
//Contributions: Alexandre Nodari.

package spikes.wheel.io.network.mocks;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;


public class Permit {
	
	boolean _isValid = true;
	private final Set<Object> _objectsToNotify = new HashSet<Object>();
	
	public void check () throws IOException {
		if (! _isValid)
			throw new IOException("Network crash simulated.");
	}
	
	public void expire() {
		_isValid = false;
		for (Object toNotify : _objectsToNotify) {
			synchronized (toNotify) {
				toNotify.notify();
			}
		}
	}

	public void addObjectToNotify(Object object) {
		_objectsToNotify.add(object);
	}
}
