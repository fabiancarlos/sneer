package wheel.testutil;

import wheel.io.Logger;

public class Profiler {

	class Worker {

		private long _timeEntered;
		private long _timeExited = -1;

		void enter() {
			_timeEntered = now();
			if (_timeExited == -1) return;
			
			addTimeOutside(_timeEntered - _timeExited);
		}

		void exit() {
			_timeExited = now();
			addTimeInside(_timeExited - _timeEntered);
		}

	}

	private final String _name;
	
	private long _totalTimeOutside = 0;
	private long _totalTimeInside = 0;

	private long _lastLogTime = 0;

	private final ThreadLocal<Worker> _worker = new ThreadLocal<Worker>();

	public Profiler(String name) {
		_name = name;
	}

	public void enter() {
		worker().enter();
	}

	private Worker worker() {
		if (_worker.get() == null)
			_worker.set(new Worker());
		
		return _worker.get();
	}

	public void exit() {
		worker().exit();
		
		logOnceInAWhile();
	}

	synchronized private void addTimeOutside(long timeOutside) {
		_totalTimeOutside += timeOutside;
	}

	synchronized private void addTimeInside(long timeInside) {
		_totalTimeInside += timeInside;
	}

	synchronized private void logOnceInAWhile() {
		if (_totalTimeOutside == 0) return;

		if (now() - _lastLogTime < 30000) return;
		_lastLogTime = now();
		
		Logger.log("{} is running during {}% of the time", _name, percentageInside());
	}

	private long percentageInside() {
		return 100 * _totalTimeInside / (_totalTimeInside + _totalTimeOutside);
	}

	private long now() {
		return System.nanoTime();
	}

}
