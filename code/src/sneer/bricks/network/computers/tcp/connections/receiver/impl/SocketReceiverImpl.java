package sneer.bricks.network.computers.tcp.connections.receiver.impl;

import static basis.environments.Environments.my;
import sneer.bricks.hardware.cpu.threads.Threads;
import sneer.bricks.network.computers.tcp.ByteArraySocket;
import sneer.bricks.network.computers.tcp.accepter.SocketAccepter;
import sneer.bricks.network.computers.tcp.connections.TcpConnectionManager;
import sneer.bricks.network.computers.tcp.connections.receiver.SocketReceiver;
import basis.lang.Closure;
import basis.lang.Consumer;

class SocketReceiverImpl implements SocketReceiver {

	private final SocketAccepter _socketAccepter;
	
	private final Threads _threads = my(Threads.class);

	@SuppressWarnings("unused") private final Object _receptionRefToAvoidGc;

	SocketReceiverImpl() {
		_socketAccepter = my(SocketAccepter.class);

		_receptionRefToAvoidGc = _socketAccepter.lastAcceptedSocket().addReceiver(new Consumer<ByteArraySocket>() { @Override public void consume(final ByteArraySocket socket) {
			_threads.startDaemon("SocketReceiverImpl", new Closure() { @Override public void run() {
				my(TcpConnectionManager.class).manageIncomingSocket(socket);
			}});
		}});
	}
}