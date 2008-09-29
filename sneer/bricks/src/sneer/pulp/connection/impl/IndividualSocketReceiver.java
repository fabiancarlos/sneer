package sneer.pulp.connection.impl;

import java.io.IOException;
import java.util.Arrays;

import sneer.kernel.container.Inject;
import sneer.pulp.connection.ConnectionManager;
import sneer.pulp.contacts.Contact;
import sneer.pulp.contacts.ContactManager;
import sneer.pulp.keymanager.KeyManager;
import sneer.pulp.keymanager.PublicKey;
import sneer.pulp.network.ByteArraySocket;
import wheel.lang.Functor;
import wheel.lang.exceptions.IllegalParameter;
import static wheel.io.Logger.log;

class IndividualSocketReceiver {

	@Inject
	static private KeyManager _keyManager;
	
	@Inject
	static private ContactManager _contactManager;
	
	@Inject
	static private ConnectionManager _connectionManager;
	
	private final ByteArraySocket _socket;
	
	IndividualSocketReceiver(ByteArraySocket socket) {
		_socket = socket;

		try {
			if (!tryToServe()) _socket.crash();
		} catch (Exception e) {
			log(e, "Exception thrown by incoming socket.");
			_socket.crash();
		}
	}

	private boolean tryToServe() throws IOException {
		shakeHands();

		PublicKey peersPublicKey = peersPublicKey();	

		if (peersPublicKey.equals(_keyManager.ownPublicKey()))
			return false;
			
		//Implement: Challenge pk.

		_socket.write(ProtocolTokens.OK);

		Contact contact = produceContact(peersPublicKey);
		_connectionManager.manageIncomingSocket(contact, _socket);
		return true;
	}

	private PublicKey peersPublicKey() throws IOException {
		byte[] publicKeyBytes = _socket.read();
		PublicKey peersPublicKey = _keyManager.unmarshall(publicKeyBytes);
		return peersPublicKey;
	}

	private Contact produceContact(PublicKey peersPublicKey) {
		return _keyManager.contactGiven(peersPublicKey, new Functor<PublicKey, Contact>(){@Override public Contact evaluate(PublicKey value) {
			return createUnconfirmedContact();
		}});
	}

	private Contact createUnconfirmedContact() {
		String baseNick = "Unconfirmed";   //"Unconfirmed (2)", "Unconfirmed (3)", etc.
		
		Contact result = tryToCreate(baseNick);
		if (result != null) return result;
		
		int i = 2;
		while (tryToCreate(baseNick + " (" + i + ")") == null)
			i++;
		
		return result;
	}

	private Contact tryToCreate(String nickname) {
		if (_contactManager.isNicknameAlreadyUsed(nickname))
			return null;
		
		try {
			return _contactManager.addContact(nickname);
		} catch (IllegalParameter e) {
			return null;
		}
	}


	private void shakeHands() throws IOException {
		while (true) {
			byte[] header = _socket.read();
			if (Arrays.equals(header, ProtocolTokens.SNEER_WIRE_PROTOCOL_1))
				return;

			discardFirstDataPacket();
			_socket.write(ProtocolTokens.FALLBACK);
		}
	}
		
	private void discardFirstDataPacket() throws IOException {
		_socket.read();
	}

	
}