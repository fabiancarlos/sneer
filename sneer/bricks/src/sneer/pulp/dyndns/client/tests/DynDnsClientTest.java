package sneer.pulp.dyndns.client.tests;

import static wheel.lang.Environments.my;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;

import sneer.kernel.container.Container;
import sneer.kernel.container.Containers;
import sneer.pulp.blinkinglights.BlinkingLights;
import sneer.pulp.blinkinglights.Light;
import sneer.pulp.clock.Clock;
import sneer.pulp.dyndns.client.DynDnsClient;
import sneer.pulp.dyndns.ownaccount.DynDnsAccount;
import sneer.pulp.dyndns.ownaccount.DynDnsAccountKeeper;
import sneer.pulp.dyndns.ownip.OwnIpDiscoverer;
import sneer.pulp.dyndns.updater.BadAuthException;
import sneer.pulp.dyndns.updater.RedundantUpdateException;
import sneer.pulp.dyndns.updater.Updater;
import sneer.pulp.dyndns.updater.UpdaterException;
import sneer.pulp.propertystore.mocks.TransientPropertyStore;
import sneer.pulp.threadpool.mocks.ThreadPoolMock;
import tests.Contribute;
import tests.TestThatIsInjected;
import wheel.lang.exceptions.FriendlyException;
import wheel.reactive.Register;
import wheel.reactive.impl.RegisterImpl;
import wheel.reactive.lists.ListSignal;

public class DynDnsClientTest extends TestThatIsInjected {
	
	/*

Required Client Behavior

    * Send a unique user agent which includes company name, model number, and software build revision.
    * Check that all input is in valid form before updating.
    * Check that any IP obtained through web-based IP detection is a valid dotted quad numeric IP (eg: 1.2.3.4) before sending it in an update.
    * Only update when the IP address is different from the IP of the last update.

Unacceptable Client Behavior

    * Send requests to or access anything other than /nic/update at the host members.dyndns.org.
    * Reverse engineer web requests to our website to create or delete hostnames.
    * Hardcode the IP address of any of DynDNS servers.
    * Attempt to update after receiving the notfqdn, abuse, nohost, badagent, badauth, badsys return codes or repeated nochg return codes without user intervention.
    * Perform DNS updates to determine whether the client IP needs to be updated.
    * Access our web-based IP detection script (http://checkip.dyndns.com/) more than once every 10 minutes

	 */
	
	final Mockery _context = new JUnit4Mockery();
	final Register<String> _ownIp = new RegisterImpl<String>("123.45.67.89");
	final DynDnsAccount _account = new DynDnsAccount("test.dyndns.org", "test", "test");
	final RegisterImpl<DynDnsAccount> _ownAccount = new RegisterImpl<DynDnsAccount>(_account);
	
	@Contribute final OwnIpDiscoverer _ownIpDiscoverer = _context.mock(OwnIpDiscoverer.class);
	@Contribute final DynDnsAccountKeeper _ownAccountKeeper = _context.mock(DynDnsAccountKeeper.class);
	@Contribute final Updater _updater = _context.mock(Updater.class);
	@Contribute final TransientPropertyStore _propertyStore = new TransientPropertyStore();
	@Contribute final ThreadPoolMock _threadPool = new ThreadPoolMock();
	
	@Test
	public void updateOnIpChange() throws Exception {
		_context.checking(new Expectations() {{
			allowing(_ownIpDiscoverer).ownIp();
				will(returnValue(_ownIp.output()));
				
			atLeast(1).of(_ownAccountKeeper).ownAccount();
				will(returnValue(_ownAccount.output()));
				
			final DynDnsAccount account = _ownAccount.output().currentValue();
			exactly(1).of(_updater).update(account.host, account.dynDnsUser, account.password, _ownIp.output().currentValue());
		}});
		

		startDynDnsClientOn(newContainer());
		
		startDynDnsClientOn(newContainer());
	}
	
	@Test
	public void retryAfterIOException() throws Exception {
		
		final IOException error = new IOException();
		
		_context.checking(new Expectations() {{
			allowing(_ownIpDiscoverer).ownIp();
				will(returnValue(_ownIp.output()));
				
			allowing(_ownAccountKeeper).ownAccount();
				will(returnValue(_ownAccount.output()));
				
			final DynDnsAccount account = _ownAccount.output().currentValue();
			exactly(1).of(_updater).update(account.host, account.dynDnsUser, account.password, _ownIp.output().currentValue());
				will(throwException(error));
				
			exactly(1).of(_updater).update(account.host, account.dynDnsUser, account.password, _ownIp.output().currentValue());
		}});
		

		startDynDnsClient();
		_threadPool.startAllActors();
		
		final Light light = assertBlinkingLight(error, my(Container.class));
		
		my(Clock.class).advanceTime(300001);
		
		_threadPool.startAllActors();
		assertFalse(light.isOn());
	}
	
	@Test
	public void userInterventionRequiredAfterFailure() throws UpdaterException, IOException {
		
		final BadAuthException error = new BadAuthException();
		final DynDnsAccount account = _ownAccount.output().currentValue();
		final String newIp = "111.111.111.111";
		
		_context.checking(new Expectations() {{
			allowing(_ownIpDiscoverer).ownIp();
				will(returnValue(_ownIp.output()));
			allowing(_ownAccountKeeper).ownAccount();
				will(returnValue(_ownAccount.output()));
			
			exactly(1).of(_updater).update(account.host, account.dynDnsUser, account.password, _ownIp.output().currentValue());
				will(throwException(error));
				
			exactly(1).of(_updater).update(account.host, account.dynDnsUser, "*" + account.password, newIp);
		}});
		
		startDynDnsClient();
		_threadPool.startAllActors();
		
		final Light light = assertBlinkingLight(error, my(Container.class));
		
		// new ip should be ignored while new account is not provided
		_ownIp.setter().consume(newIp);
		
		DynDnsAccount changed = new DynDnsAccount("test.dyndns.org", "test", "*test");
		_ownAccount.setter().consume(changed);

		_threadPool.startAllActors();
		assertFalse(light.isOn());
		
	}

	@Test
	public void redundantUpdate() throws UpdaterException, IOException {
		
		final RedundantUpdateException error = new RedundantUpdateException();
		final DynDnsAccount account = _ownAccount.output().currentValue();
		
		_context.checking(new Expectations() {{
			allowing(_ownIpDiscoverer).ownIp();	will(returnValue(_ownIp.output()));
			allowing(_ownAccountKeeper).ownAccount(); will(returnValue(_ownAccount.output()));
			
			exactly(1).of(_updater).update(account.host, account.dynDnsUser, account.password, _ownIp.output().currentValue());
				will(throwException(error));
		}});
		
		startDynDnsClient();
		_threadPool.startAllActors();
		
		assertBlinkingLight(error, my(Container.class));
	}

	
	private Light assertBlinkingLight(final Exception expectedError, final Container container) {
		final ListSignal<Light> lights = container.provide(BlinkingLights.class).lights();
		assertEquals(1, lights.currentSize());
		final Light light = lights.currentGet(0);
		assertTrue(light.isOn());
		if (expectedError instanceof FriendlyException) {
			assertEquals(((FriendlyException)expectedError).getHelp(), light.helpMessage());
		}
		assertSame(expectedError, light.error());
		return light;
	}

	private Container startDynDnsClient() {
		final Container container = my(Container.class);
		return startDynDnsClientOn(container);
	}

	private Container startDynDnsClientOn(final Container container) {
		container.provide(DynDnsClient.class);
		return container;
	}

	private Container newContainer(Object...mocks) {
		List<Object> list = new ArrayList<Object>();
		for (Object mock : mocks) {
			list.add(mock);
		}
		list.add(_ownIpDiscoverer);
		list.add(_ownAccountKeeper);
		list.add(_updater);
		list.add(_propertyStore);
		
		return Containers.newContainer(list.toArray());
	}
}

