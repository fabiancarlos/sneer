package sneer.pulp.streams.sequencer.tests;

import java.util.ArrayList;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;
import org.junit.runner.RunWith;

import sneer.kernel.container.Inject;
import sneer.pulp.clock.Clock;
import sneer.pulp.keymanager.KeyManager;
import sneer.pulp.keymanager.PublicKey;
import sneer.pulp.streams.sequencer.SpeakerBuffer;
import sneer.pulp.streams.sequencer.SpeakerBuffers;
import sneer.skin.sound.PcmSoundPacket;
import tests.JMockContainerEnvironment;
import tests.TestThatIsInjected;
import wheel.lang.Consumer;
import wheel.lang.ImmutableByteArray;

@RunWith(JMockContainerEnvironment.class)
public class SpeakerBufferTest extends TestThatIsInjected {
	
	@Inject private static SpeakerBuffers _subject;
	
	@Inject private static Clock _clock;
	
	@Inject private static KeyManager _keyManager;

	private final List<Short> _recordedSequence = new ArrayList<Short>();

	private final Mockery _mockery = new JUnit4Mockery();
	
	private final Consumer<PcmSoundPacket> _consumer = _mockery.mock(Consumer.class);

	@Test
	public void correctOrder() throws Exception {
		testBuffering(p1(), p2());
	}

	@Test
	public void inverseOrder() throws Exception {
		testBuffering(p2(), p1());
	}

	private void testBuffering(PcmSoundPacket first, PcmSoundPacket second) {
		_mockery.checking(new Expectations(){{
			Sequence main = _mockery.sequence("main");
			one(_consumer).consume(p0()); inSequence(main);
			one(_consumer).consume(p1()); inSequence(main);
			one(_consumer).consume(p2()); inSequence(main);
			one(_consumer).consume(p3()); inSequence(main);
		}});
		
		SpeakerBuffer buffer = _subject.createBufferFor(_consumer);
		PcmSoundPacket initialPacket = p0();
		PcmSoundPacket lastPacket = p3();

		buffer.consume(initialPacket);
		buffer.consume(first);
		buffer.consume(second);
		buffer.consume(lastPacket);
	}
	
	@Test
	public void sequencing() {
		int[] input = new int[] {
			0, 1, 2, 3, //Happy
			-1, 2, // Less than 500 negative difference: discarded
			6, 4, 5, //Different order, no gap
			7, 11, 10, 9, 8, //Different order, no gap
			20, 23, 21, //Different order, with gap: will wait
			23, // Already waiting: discard
			51, // More than 30 gap since last played (11): will cause 20, 21 to be played.
			600, 601, 602, // More than 500 gap: will cause buffer to drain (23, 51 will not be played)
			604, //Gap
			-3, -2, -1, // More than 500 gap in the other direction will also cause buffer to drain (604 will not be played)
			Short.MAX_VALUE-2, Short.MAX_VALUE, Short.MAX_VALUE-1,
			Short.MIN_VALUE+2, Short.MIN_VALUE, Short.MIN_VALUE +1
		};

		feedInputSequence(input);
		expectOutputSequence(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 20, 21, 600, 601, 602, -3, -2, -1,
				Short.MAX_VALUE-2, Short.MAX_VALUE-1, Short.MAX_VALUE,
				Short.MIN_VALUE, Short.MIN_VALUE+1, Short.MIN_VALUE+2);
	}

	private void feedInputSequence(int[] input) {
		SpeakerBuffer buffer = _subject.createBufferFor(sequenceRecorder());
		
		for (int sequence : input)
			buffer.consume(packet(sequence));
	}
	
	private Consumer<PcmSoundPacket> sequenceRecorder() {
		return new Consumer<PcmSoundPacket>(){ @Override public void consume(PcmSoundPacket value) {
			_recordedSequence.add(value.sequence);
		}};
	}
	
	private void expectOutputSequence(int... sequences) {
		for (int i = 0; i < sequences.length; i++)
			assertEquals((int)_recordedSequence.get(i), (Object)sequences[i]); 
	}
	
	private PcmSoundPacket packet(int sequence) {
		return contactPacket(new byte[] { (byte) 7, 11, 13, 17 }, (short)sequence);
	}

	@SuppressWarnings("deprecation")
	private PublicKey contactKey() {
		return _keyManager.generateMickeyMouseKey("contact");
	}

	private PcmSoundPacket contactPacket(byte[] pcm, short sequence) {
		return pcmSoundPacketFor(contactKey(), pcm, sequence);
	}
	
	private PcmSoundPacket pcmSoundPacketFor(PublicKey publicKey, final byte[] pcmPayload, short sequence) {
		return new PcmSoundPacket(publicKey, _clock.time(), new ImmutableByteArray(pcmPayload, pcmPayload.length), sequence);
	}
	
	private PcmSoundPacket p0() {
		return packet(0);
	}
	
	private PcmSoundPacket p1() {
		return packet(1);
	}
	
	private PcmSoundPacket p2() {
		return packet(2);
	}
	
	private PcmSoundPacket p3() {
		return packet(3);
	}
}