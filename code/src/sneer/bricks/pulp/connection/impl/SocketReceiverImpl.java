package sneer.bricks.pulp.connection.impl;

import static sneer.foundation.environments.Environments.my;
import sneer.bricks.hardware.cpu.lang.Consumer;
import sneer.bricks.pulp.connection.SocketAccepter;
import sneer.bricks.pulp.connection.SocketReceiver;
import sneer.bricks.pulp.network.ByteArraySocket;
import sneer.bricks.pulp.reactive.Signals;
import sneer.bricks.pulp.threads.Stepper;
import sneer.bricks.pulp.threads.Threads;

class SocketReceiverImpl implements SocketReceiver {

	private final SocketAccepter _socketAccepter = my(SocketAccepter.class);
	
	private final Threads _threads = my(Threads.class);

	@SuppressWarnings("unused") private final Object _receptionRefToAvoidGc;

	private Stepper _stepperRefToAvoidGc;

	SocketReceiverImpl() {
		_receptionRefToAvoidGc = my(Signals.class).receive(_socketAccepter.lastAcceptedSocket(), new Consumer<ByteArraySocket>() { @Override public void consume(final ByteArraySocket socket) {
			_stepperRefToAvoidGc = new Stepper() { @Override public boolean step() {
				new IndividualSocketReceiver(socket);
				return false;
			}};

			_threads.registerStepper(_stepperRefToAvoidGc);
		}});
	}
}