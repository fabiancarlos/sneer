package sneer.bricks.hardware.cpu.threads.mocks;

import static basis.environments.Environments.my;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import basis.brickness.impl.BricknessImpl;
import basis.lang.Closure;

import sneer.bricks.hardware.cpu.lang.contracts.Contract;
import sneer.bricks.hardware.cpu.threads.Threads;
import sneer.bricks.pulp.notifiers.Notifier;
import sneer.bricks.pulp.notifiers.Notifiers;
import sneer.bricks.pulp.notifiers.pulsers.Pulser;

public class ThreadsMock implements Threads {

	private final Threads _delegate = my(BricknessImpl.class).provide(Threads.class);
	
	List<Closure> _steppers = new ArrayList<Closure>();
	
	private final String _daemonNameFragmentToHold;
	private Map<Runnable, String> _daemonNamesByRunnable = new HashMap<Runnable, String>();

	private final Notifier<Object> _crashingPulser = my(Notifiers.class).newInstance();


	public ThreadsMock(String daemonNameFragmentToHold) {
		_daemonNameFragmentToHold = daemonNameFragmentToHold;
	}
	

	@Override
	public synchronized Contract startStepping(Closure stepper) {
		_steppers.add(stepper);
		return null;
	}

	
	@Override
	public synchronized Contract startStepping(String threadNameIgnored, Closure stepper) {
		return startStepping(stepper);
	}

	public synchronized Closure getStepper(int i) {
		return _steppers.get(i);
	}

	public synchronized void runDaemonWithNameContaining(String partOfName) {
		Collection<Runnable> daemonsCopy = new ArrayList<Runnable>(_daemonNamesByRunnable.keySet());

		boolean wasRun = false;
		
		for (Runnable daemon : daemonsCopy) {
			String daemonName = _daemonNamesByRunnable.get(daemon);
			if (daemonName.indexOf(partOfName) == -1) continue;
			
			_daemonNamesByRunnable.remove(daemon);
			daemon.run();
			if (wasRun) throw new IllegalStateException("Found more than one daemon named: " + partOfName);
			wasRun = true;
		}
		
		if (!wasRun) throw new IllegalStateException("Daemon not found: " + partOfName);
	}

	@Override
	public void joinWithoutInterruptions(Thread thread) {
		throw new basis.lang.exceptions.NotImplementedYet(); // Implement
	}


	@Override
	public void sleepWithoutInterruptions(long milliseconds) {
		try {
			Thread.sleep(milliseconds);
		} catch (InterruptedException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void startDaemon(String threadName, Closure closure) {
		if (threadName.indexOf(_daemonNameFragmentToHold) == -1)
			_delegate.startDaemon(threadName, closure);
		else
			_daemonNamesByRunnable.put(closure, threadName);
	}

	
	@Override
	public void waitWithoutInterruptions(Object object) {
		try {
			object.wait();

		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void waitUntilCrash() {
		throw new basis.lang.exceptions.NotImplementedYet(); // Implement
	}

	@Override
	public void crashAllThreads() {
		_crashingPulser.notifyReceivers(null);
	}

	@Override
	public Pulser crashed() {
		return _crashingPulser.output();
	}

}