package sneer.pulp.transientpropertystore.tests;

import static sneer.commons.environments.Environments.my;

import org.junit.Ignore;
import org.junit.Test;

import sneer.brickness.Brickness;
import sneer.brickness.testsupport.TestThatMightUseResources;
import sneer.commons.environments.Environment;
import sneer.commons.environments.Environments;
import sneer.pulp.transientpropertystore.TransientPropertyStore;
import sneer.pulp.transientpropertystore2.TransientPropertyStore2;

public class TransparentPropertyStoreTest extends TestThatMightUseResources {

	@Test
	@Ignore
	public void testPropertyStore() {
		runInNewContainer(new Runnable() { @Override public void run() {
			TransientPropertyStore subject1 = my(TransientPropertyStore.class);
			assertNull(subject1.get("Height"));
			subject1.set("Height", "1,80m");
			subject1.set("Weight", "85kg");
			assertEquals("1,80m", subject1.get("Height"));
			assertEquals("85kg", subject1.get("Weight"));
			
			TransientPropertyStore2 subject2 = my(TransientPropertyStore2.class);
			
			subject2.setSuffix("2");
			subject2.set("Msg", "Hi!");
			assertEquals("Hi!2", subject1.get("Msg"));
			
			subject2.setSuffix("World!");
			subject2.set("Msg", "Hi!");
			assertEquals("Hi!World!", subject1.get("Msg"));
			
		}});
		
		runInNewContainer(new Runnable() { @Override public void run() {
			TransientPropertyStore subject1 = my(TransientPropertyStore.class);
			assertEquals("1,80m", subject1.get("Height"));
			assertEquals("85kg", subject1.get("Weight"));
			assertEquals("Hi!World!", subject1.get("Msg"));
			
			TransientPropertyStore2 subject2 = my(TransientPropertyStore2.class);
			assertEquals("World!", subject2.suffix());
			
		}});
	}

	private void runInNewContainer(Runnable runnable) {
		Environment container = Brickness.newBrickContainer();
		Environments.runWith(container, runnable);
	}


}
