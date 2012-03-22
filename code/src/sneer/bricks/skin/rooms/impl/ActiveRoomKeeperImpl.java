package sneer.bricks.skin.rooms.impl;

import basis.lang.Consumer;
import sneer.bricks.pulp.reactive.Register;
import sneer.bricks.pulp.reactive.Signal;
import sneer.bricks.pulp.reactive.Signals;
import sneer.bricks.skin.rooms.ActiveRoomKeeper;
import static basis.environments.Environments.my;

class ActiveRoomKeeperImpl implements ActiveRoomKeeper {

	private final Register<String> _register = my(Signals.class).newRegister("");

	@Override
	public Signal<String> room() {
		return _register.output();
	}

	@Override
	public Consumer<String> setter() {
		return _register.setter();
	}

}
