package sneer.bricks.identity.keys.own.impl;

import static basis.environments.Environments.my;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import sneer.bricks.hardware.cpu.crypto.Crypto;
import sneer.bricks.identity.keys.own.OwnKeys;
import sneer.bricks.pulp.reactive.Register;
import sneer.bricks.pulp.reactive.Signal;
import sneer.bricks.pulp.reactive.Signals;

class OwnKeysImpl implements OwnKeys {
	
	private final Register<PublicKey> _ownPublicKey = my(Signals.class).newRegister(null);
	private PrivateKey _ownPrivateKey;

	
	@Override
	public void generateKeyPair(byte[] seed) {
		if(_ownPrivateKey != null) throw new IllegalStateException("Private key has already been generated.");
		
		KeyPair newPair = my(Crypto.class).newECDSAKeyPair(seed);
		
		_ownPublicKey.setter().consume(newPair.getPublic());
		_ownPrivateKey = newPair.getPrivate();
	}

	
	@Override
	public Signal<PublicKey> ownPublicKey() {
		return _ownPublicKey.output();
	}

}
