package sneer.pulp.tuples.tests;

import static sneer.commons.environments.Environments.my;

import java.util.ArrayList;

import org.junit.Test;

import sneer.brickness.Tuple;
import sneer.brickness.testsupport.BrickTest;
import sneer.hardware.cpu.lang.Consumer;
import sneer.pulp.tuples.TupleSpace;

public class TupleSpaceTest extends BrickTest {

	private final TupleSpace _subject = my(TupleSpace.class);
	
	private TestTuple _received;

	
	@Test
	public void tuplesContainingArrays() {
		TestTuple a = new TestTuple(new int[]{1, 2, 3});
		TestTuple b = new TestTuple(new int[]{1, 2, 3});
		assertTrue(a.hashCode() == b.hashCode());
		assertEquals(a, b);

		Consumer<TestTuple> refToAvoidGc = new Consumer<TestTuple>(){@Override public void consume(TestTuple received) {
			_received = received;
		}};
		_subject.addSubscription(TestTuple.class, refToAvoidGc);
		
		_subject.publish(a);
		_subject.waitForAllDispatchingToFinish();
		assertEquals(_received, a);
		
		_received = null;
		_subject.publish(b);
		assertNull(_received);
	}
	
	
	@Test
	public void subscriptionRemoval() {
		final ArrayList<Tuple> tuples = new ArrayList<Tuple>();
		final Consumer<TestTuple> consumer = new Consumer<TestTuple>() { @Override public void consume(TestTuple value) {
			tuples.add(value);
		}};
		_subject.addSubscription(TestTuple.class, consumer);
		
		final TestTuple tuple = new TestTuple(42);
		_subject.publish(tuple);
		_subject.removeSubscriptionAsync(consumer);
		_subject.publish(new TestTuple(-1));
		my(TupleSpace.class).waitForAllDispatchingToFinish();
		assertArrayEquals(new Object[] { tuple }, tuples.toArray());
	}
	
}


