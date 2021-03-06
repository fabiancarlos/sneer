package sneer.bricks.network.computers.tcp.connections.impl;

import static basis.environments.Environments.my;

import java.io.IOException;

import sneer.bricks.hardware.cpu.lang.contracts.WeakContract;
import sneer.bricks.hardware.cpu.threads.Threads;
import sneer.bricks.identity.seals.Seal;
import sneer.bricks.network.computers.connections.Call;
import sneer.bricks.network.computers.tcp.ByteArraySocket;
import sneer.bricks.network.computers.tcp.connections.TcpConnectionManager;
import sneer.bricks.network.social.Contact;
import sneer.bricks.pulp.notifiers.Source;
import basis.lang.ClosureX;

class TcpConnectionManagerImpl implements TcpConnectionManager {
	
	static final WeakContract crashingContract = my(Threads.class).crashed().addPulseReceiver(new Runnable() { @Override public void run() {
		for (ByteConnectionImpl victim : ConnectionsByContact.all())
			victim.close();
	}});

	
	@Override
	public ByteConnectionImpl connectionFor(final Contact contact) {
		return ConnectionsByContact.get(contact);
	}

	
	@Override
	public void manageIncomingSocket(final ByteArraySocket socket) {
		manageSocket(socket, "Incoming", new ClosureX<IOException>() { @Override public void run() throws IOException {
			Seal contactsSeal = IncomingHandShaker.greet(socket);
			TieBreaker.manageIncomingSocket(socket, contactsSeal);
		}});
	}


	@Override
	public void manageOutgoingSocket(final ByteArraySocket socket, final Contact contact) {
		manageSocket(socket, "Outgoing", new ClosureX<IOException>() { @Override public void run() throws IOException {
			OutgoingHandShaker.greet(socket, contact);
			TieBreaker.manageOutgoingSocket(socket, contact);
		}});
	}

	
	private void manageSocket(final ByteArraySocket socket, String direction, ClosureX<IOException> closure) {
		SocketCloser.closeIfUnsuccessful(socket, direction + " socket closed.", closure);
		
//		if (my(Threads.class).isCrashing())
//			SocketCloser.close(socket, "Closing socket that was " + direction + " while crashing all threads.");
	}

	
	@Override
	public void closeConnectionFor(Contact contact) {
		ByteConnectionImpl connection = ConnectionsByContact.remove(contact);
		if (connection != null) connection.close();
	}


	@Override
	public Source<Call> unknownCallers() {
		return IncomingHandShaker.unknownCallers();
	}

}