package sneer.bricks.network.computers.tcp.accepter.impl;

import static basis.environments.Environments.my;

import java.io.IOException;

import basis.lang.Closure;
import basis.lang.Consumer;

import sneer.bricks.hardware.cpu.lang.contracts.Contract;
import sneer.bricks.hardware.cpu.lang.contracts.WeakContract;
import sneer.bricks.hardware.cpu.threads.Threads;
import sneer.bricks.hardware.io.log.Logger;
import sneer.bricks.network.computers.ports.OwnPort;
import sneer.bricks.network.computers.tcp.ByteArrayServerSocket;
import sneer.bricks.network.computers.tcp.ByteArraySocket;
import sneer.bricks.network.computers.tcp.TcpNetwork;
import sneer.bricks.network.computers.tcp.accepter.SocketAccepter;
import sneer.bricks.network.social.attributes.Attributes;
import sneer.bricks.pulp.blinkinglights.BlinkingLights;
import sneer.bricks.pulp.blinkinglights.Light;
import sneer.bricks.pulp.blinkinglights.LightType;
import sneer.bricks.pulp.notifiers.Notifier;
import sneer.bricks.pulp.notifiers.Notifiers;
import sneer.bricks.pulp.notifiers.Source;
import sneer.bricks.pulp.reactive.Signal;

class SocketAccepterImpl implements SocketAccepter {
	private final Signal<Integer> _ownPort = my(Attributes.class).myAttributeValue(OwnPort.class);
	private final TcpNetwork _network = my(TcpNetwork.class);
	private final BlinkingLights _lights = my(BlinkingLights.class);
	private final Threads _threads = my(Threads.class);
	@SuppressWarnings("unused")
	private final WeakContract _crashingContract = _threads.crashed().addPulseReceiver(new Closure() { @Override public void run() {
		crashServerSocketIfNecessary();
	}});
	
	private final Notifier<ByteArraySocket> _notifier = my(Notifiers.class).newInstance();

	private final transient Object _portToListenMonitor = new Object();

	private ByteArrayServerSocket _serverSocket;
	
	private int _portToListen;
	
	private Light _cantOpenServerSocket = _lights.prepare(LightType.ERROR);

	private final Light _cantAcceptSocket = _lights.prepare(LightType.ERROR);

	@SuppressWarnings("unused") private final Object _receptionRefToAvoidGc;
	private Contract _stepperContract;

	SocketAccepterImpl() {
		_receptionRefToAvoidGc = _ownPort.addReceiver(new Consumer<Integer>() { @Override public void consume(Integer port) {
			setPort(port == null ? 0 : port);
		}});

		_threads.startStepping(new Closure() { @Override public void run() {
			listenToSneerPort();
		}});
	}

	@Override
    public Source<ByteArraySocket> lastAcceptedSocket() {
    	return _notifier.output();
    }

    private void setPort(int port) {
		synchronized (_portToListenMonitor) {
			_portToListen = port;
			_portToListenMonitor.notify();
		}
	}

    private void listenToSneerPort() {
		int myPortToListen = _portToListen;
		crashServerSocketIfNecessary();
		openServerSocket(myPortToListen);	

		if(_serverSocket != null) startAccepting();

		synchronized (_portToListenMonitor) {
			if (myPortToListen == _portToListen)
				_threads.waitWithoutInterruptions(_portToListenMonitor);
		}
    }
	
	private void startAccepting() {
		_stepperContract = _threads.startStepping(new Closure() { @Override public void run() {
			try {
				dealWith(_serverSocket.accept());
			} catch (IOException e) {
				dealWith(e);
			}
		}});

	}
	
	private void dealWith(ByteArraySocket incomingSocket) {
		_lights.turnOffIfNecessary(_cantAcceptSocket);
		_notifier.notifyReceivers(incomingSocket);
	}

	private void dealWith(IOException e) {
		if (_stepperContract != null) 
			_lights.turnOnIfNecessary(_cantAcceptSocket, "Unable to accept client connection", null, e);
	}

	private void openServerSocket(int port) {
		if (port <= 0) return;
		try {
			_serverSocket = _network.openServerSocket(port);
			_lights.turnOffIfNecessary(_cantOpenServerSocket);
			_lights.turnOn(LightType.GOOD_NEWS, "TCP port opened: " + port, "Sneer has successfully opened TCP port " + port + " to receive incoming connections from others.", 7000);
		} catch (IOException e) {
			if (_stepperContract != null)
				_lights.turnOnIfNecessary(_cantOpenServerSocket, "Unable to listen on TCP port " + port, helpMessage(), e);
		}	
	}

	private String helpMessage() {
		return "Typical causes:\n" +
			"- You might have another Sneer instance already running\n" +
			"- Some other application is already using that port\n" +
			"- Your operating system or firewall is blocking that port, especially if it is below 1024\n" +
			"\n" +
			"You can run multiple Sneer instances on the same machine but each has to use a separate TCP port.";
	}

	private void crashServerSocketIfNecessary() {
		if(_serverSocket == null) return;

		my(Logger.class).log("crashing server socket");
		if (_stepperContract != null) _stepperContract.dispose();
		_stepperContract = null;
		_serverSocket.crash();
	}
}
