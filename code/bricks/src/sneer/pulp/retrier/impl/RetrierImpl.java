package sneer.pulp.retrier.impl;

import static sneer.commons.environments.Environments.my;
import sneer.hardware.cpu.exceptions.FriendlyException;
import sneer.hardware.cpu.exceptions.Hiccup;
import sneer.pulp.blinkinglights.BlinkingLights;
import sneer.pulp.blinkinglights.Light;
import sneer.pulp.blinkinglights.LightType;
import sneer.pulp.clock.Clock;
import sneer.pulp.retrier.Retrier;
import sneer.pulp.retrier.Task;
import sneer.pulp.threads.Stepper;
import sneer.pulp.threads.Threads;

class RetrierImpl implements Retrier {

	private final Threads _threads = my(Threads.class);
	private final BlinkingLights _lights = my(BlinkingLights.class);
	private final Clock _clock = my(Clock.class);
	
	
	private volatile boolean _isStillTrying = true;
	private final Light _light = _lights.prepare(LightType.ERROR);

	
	RetrierImpl(final int periodBetweenAttempts, final Task task) {
		_threads.registerStepper(new Stepper() { @Override public boolean step() {
			if (wasSuccessful(task))
				return false;
			_clock.sleepAtLeast(periodBetweenAttempts);
			return true;
		}});
	}

	
	@Override
	synchronized public void giveUpIfStillTrying() {
		_isStillTrying = false;
		turnLightOff();
	}

	
	private void turnLightOff() {
		_lights.turnOffIfNecessary(_light);
	}


	private boolean wasSuccessful(final Task task) {
		try {
			if (_isStillTrying) task.execute();
		} catch (Hiccup ok) {
		} catch (FriendlyException fx) {
			dealWith(fx);
			return false;
		}
		
		turnLightOff();
		return true;
	}

	synchronized private void dealWith(FriendlyException fx) {
		if (!_isStillTrying) return;
		_lights.turnOnIfNecessary(_light, fx);
	}

}