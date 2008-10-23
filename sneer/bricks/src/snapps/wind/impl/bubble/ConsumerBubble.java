package snapps.wind.impl.bubble;

import java.util.List;

import org.prevayler.Prevayler;

import wheel.lang.Consumer;
import wheel.lang.exceptions.IllegalParameter;


@SuppressWarnings("unchecked")
class ConsumerBubble implements Consumer {

	private final List<String> _getterPathToConsumer;
	private final Prevayler _prevayler;

	ConsumerBubble(Prevayler prevayler, List<String> getterPathToConsumer) {
		_getterPathToConsumer = getterPathToConsumer;
		_prevayler = prevayler;
	}

	public void consume(Object vo) throws IllegalParameter {
		try {
			_prevayler.execute(new Consumption(_getterPathToConsumer, vo));
		} catch (IllegalParameter e) {
			throw e;
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

}