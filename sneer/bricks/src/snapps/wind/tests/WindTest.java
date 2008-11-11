package snapps.wind.tests;

import org.junit.Test;

import snapps.wind.Shout;
import snapps.wind.Wind;
import sneer.kernel.container.Inject;
import sneer.kernel.container.tests.TestThatIsInjected;
import sneer.pulp.tuples.TupleSpace;

public class WindTest extends TestThatIsInjected {

	@Inject
	private static TupleSpace _tupleSpace;
	
	@Inject
	private static Wind _wind;
	
	@Test
	public void testSortedShoutsHeard() {
		
		_tupleSpace.publish(new ShoutMock(""+15, 15));

		for (int i = 30; i > 20; i--) {
			ShoutMock shout = new ShoutMock(""+i, i);
			_tupleSpace.publish(shout);
		}
		
		for (int i = 10; i > 0; i--) {
			ShoutMock shout = new ShoutMock(""+i, i);
			_tupleSpace.publish(shout);
		}
		
		Shout previusShout = null;
		for (Shout _shout : _wind.shoutsHeard()) {
			
			if(previusShout==null){
				previusShout = _shout;
				continue;
			}
			
			assertTrue(previusShout.publicationTime() < _shout.publicationTime());
			previusShout = _shout;
		}
	}
}