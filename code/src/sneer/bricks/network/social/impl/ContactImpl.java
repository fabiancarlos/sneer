package sneer.bricks.network.social.impl;

import static basis.environments.Environments.my;
import sneer.bricks.network.social.Contact;
import sneer.bricks.pulp.reactive.Register;
import sneer.bricks.pulp.reactive.Signal;
import sneer.bricks.pulp.reactive.Signals;

class ContactImpl implements Contact {

	private final Register<String> _nickname;
	
	public ContactImpl(String nickname) {
		if (nickname == null)
			throw new IllegalArgumentException("Nickname cannot be null");

		_nickname = my(Signals.class).newRegister(nickname);
	}
	
	@Override
	public Signal<String> nickname() {
		return _nickname.output();
	}
	
	@Override
	public String toString() {
		return _nickname.output().currentValue();
	}

	void setNickname(String newNickname) {
		_nickname.setter().consume(newNickname);
	}
	
}