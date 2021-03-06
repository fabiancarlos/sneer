package dfcsantos.music.tests;

import static basis.environments.Environments.my;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.jmock.Expectations;
import org.junit.Ignore;
import org.junit.Test;

import basis.brickness.testsupport.Bind;
import basis.environments.Environment;
import basis.environments.Environments;
import basis.lang.ClosureX;
import basis.lang.Functor;

import sneer.bricks.expression.tuples.testsupport.BrickTestWithTuples;
import sneer.bricks.expression.tuples.testsupport.pump.TuplePump;
import sneer.bricks.hardware.clock.ticker.custom.CustomClockTicker;
import sneer.bricks.hardware.io.IO;
import sneer.bricks.hardware.ram.collections.CollectionUtils;
import sneer.bricks.pulp.blinkinglights.BlinkingLights;
import sneer.bricks.pulp.blinkinglights.Light;
import sneer.bricks.pulp.reactive.Signal;
import sneer.bricks.pulp.reactive.SignalUtils;
import dfcsantos.music.Music;
import dfcsantos.music.Music.OperatingMode;
import dfcsantos.tracks.Track;
import dfcsantos.tracks.execution.player.TrackPlayer;
import dfcsantos.tracks.storage.folder.TracksFolderKeeper;

public class MusicFunctionalTest extends BrickTestWithTuples {
	
	private Music _subject1;
	private Music _subject2;

	@Bind private TrackPlayer _trackPlayer = mock(TrackPlayer.class);
	
	@SuppressWarnings("unused")	private TuplePump _refToAvoidGc;
	

	@Test (timeout = 2000)
	public void basicStuff() {
		_subject1 = my(Music.class);

		assertEquals(null, _subject1.playingFolder());
		assertEquals(null, _subject1.tracksFolder().currentValue());

		assertEquals(_subject1.operatingMode().currentValue(), OperatingMode.OWN);
		_subject1.setOperatingMode(OperatingMode.PEERS);
		waitForSignalValue(_subject1.operatingMode(), OperatingMode.PEERS);

		_subject1.skip();
		assertEquals(_subject1.playingTrack().currentValue(), null);
		assertEquals(_subject1.isPlaying().currentValue(), false);

		assertTrue(_subject1.numberOfOwnTracks().currentValue() == 0);
		assertTrue(_subject1.numberOfPeerTracks().currentValue() == 0);
		assertTrue(_subject1.isTrackExchangeActive().output().currentValue());

		_subject1.isTrackExchangeActive().setter().consume(true);
		waitForSignalValue(_subject1.isTrackExchangeActive().output(), true);		
	}

	
	@Test (timeout = 3000)
	public void ownModeWithOneTrack() throws IOException {
		_subject1 = my(Music.class);
		_subject1.setPlayingFolder(tmpFolder());
		createSampleTracks(_subject1.playingFolder(), "track1.mp3");

		checking(new Expectations() {{
			exactly(4).of(_trackPlayer).startPlaying(with(any(Track.class)), with(any(Signal.class)), with(any(Signal.class)), with(any(Runnable.class)));
		}});

		_subject1.skip(); // Starts 1st TrackContract
		waitForSignalValue(_subject1.isPlaying(), true);
		assertEquals("track1", playingTrack());

		_subject1.skip(); // Starts 2nd TrackContract
		waitForSignalValue(_subject1.isPlaying(), true);
		assertEquals("track1", playingTrack());

		_subject1.skip(); // Starts 3rd TrackContract
		waitForSignalValue(_subject1.isPlaying(), true);
		assertEquals("track1", playingTrack());

		_subject1.stop();
		waitForSignalValue(_subject1.isPlaying(), false);

		_subject1.pauseResume(); // Starts 4th TrackContract
		waitForSignalValue(_subject1.isPlaying(), true);
		assertEquals("track1", playingTrack());

		_subject1.pauseResume();
		waitForSignalValue(_subject1.isPlaying(), false);

		_subject1.pauseResume();
		waitForSignalValue(_subject1.isPlaying(), true);
		assertEquals("track1", playingTrack());

		_subject1.noWay();
		waitForSignalValue(_subject1.isPlaying(), false);

		_subject1.pauseResume();
		assertLightExists("No Tracks to Play");
	}


	private void assertLightExists(String caption) {
		for (Light light : my(BlinkingLights.class).lights())
			if (caption.equals(light.caption()))
				return;
		fail("Light not found: '" +caption + "'");
	}

	
	@Test (timeout = 2000)
	public void ownModeWithMultipleTracks() throws IOException {
		/*	Folder structure created:
		 *
		 *	tmpFolder()
		 * 		|_ tmp/media/tracks (Default Playing Folder)
		 * 				|_ music.mp3
		 * 				|_ subdirectory1
		 * 						|_ track1.mp3
		 * 						|_ track2.mp3
		 * 				|_ subdirectory2
		 * 						|_ track3.mp3
		 * 						|_ track4.mp3
		 * 				|_ track5.mp3
		 * 				|_ track6.mp3
		 */

		_subject1 = my(Music.class);
		_subject1.setPlayingFolder(tmpFolder());
		
		File rootFolder = _subject1.playingFolder();
		createSampleTracks(rootFolder, "track5.mp3", "track6.mp3");

		File subdirectory1 = new File(rootFolder, "subdirectory1");
		createSampleTracks(subdirectory1, "track1.mp3", "track2.mp3");

		File subdirectory2 = new File(rootFolder, "subdirectory2");
		createSampleTracks(subdirectory2, "track3.mp3", "track4.mp3");

		checking(new Expectations() {{
			allowing(_trackPlayer).startPlaying(with(any(Track.class)), with(any(Signal.class)), with(any(Signal.class)), with(any(Runnable.class)));
		}});

		// Play all songs sequentially
		_subject1.shuffle().setter().consume(false);
		_subject1.skip();
		assertEquals("track1", playingTrack());
		_subject1.skip();
		assertEquals("track2", playingTrack());
		_subject1.skip();
		assertEquals("track3", playingTrack());
		_subject1.skip();
		assertEquals("track4", playingTrack());
		_subject1.skip();
		assertEquals("track5", playingTrack());
		_subject1.skip();
		assertEquals("track6", playingTrack());
		_subject1.skip(); // Playlist reloaded
		assertEquals("track1", playingTrack());

		// Play only the songs from subdirectory1
		_subject1.setPlayingFolder(subdirectory1);
		assertEquals(subdirectory1, _subject1.playingFolder());
		assertEquals("track1", playingTrack());
		_subject1.skip();
		assertEquals("track2", playingTrack());
		_subject1.skip();
		assertEquals("track1", playingTrack());

		// Play all songs randomly
		_subject1.setPlayingFolder(rootFolder);
		_subject1.shuffle().setter().consume(true);

		// Pseudo-random sequence (found by regression)
		_subject1.skip();
		assertEquals("track5", playingTrack());
		_subject1.skip();
		assertEquals("track2", playingTrack());
		_subject1.skip();
		assertEquals("track3", playingTrack());
		_subject1.skip();
		assertEquals("track6", playingTrack());
		_subject1.skip();
		assertEquals("track4", playingTrack());
		_subject1.skip();
		assertEquals("track1", playingTrack());
		_subject1.skip(); // Playlist reloaded
		assertEquals("track5", playingTrack());
	}

	
	@Ignore
	@Test (timeout = 4000)
	public void peersMode() throws IOException {
		activateTrackEndorsementsFrom(remote());

		_subject1 = my(Music.class);
		_subject1.isTrackExchangeActive().setter().consume(true);

		my(CustomClockTicker.class).start(10, 100);

		waitForSignalValue(_subject1.numberOfPeerTracks(), 3);

		checking(new Expectations() {{
			exactly(3).of(_trackPlayer).startPlaying(with(any(Track.class)), with(any(Signal.class)), with(any(Signal.class)), with(any(Runnable.class)));
		}});

		_subject1.setOperatingMode(OperatingMode.PEERS);

		// Deletes first played track
		_subject1.skip();
		_subject1.noWay(); // Skip is called automatically after a track is deleted

		File[] keptTracks = new File[2];
		keptTracks[0] = _subject1.playingTrack().currentValue().file();
		_subject1.meToo(); // Keeps second played track
		assertTrue(_subject1.isPlaying().currentValue()); // MeToo doesn't affect the playing track's flow

		_subject1.skip();
		keptTracks[1] = _subject1.playingTrack().currentValue().file();
		_subject1.meToo(); // Keeps last played track

		waitForSignalValue(_subject1.numberOfPeerTracks(), 0);
		_subject1.skip();
		assertFalse(_subject1.isPlaying().currentValue());

		File[] novelties = my(TracksFolderKeeper.class).noveltiesFolder().listFiles();
		assertEquals(2, novelties.length);
		assertContentsInAnyOrder(trackNames(keptTracks), trackNames(novelties).toArray(new String[0]));
	}


	private <T> void waitForSignalValue(Signal<T> signal, T value) {
		my(SignalUtils.class).waitForValue(signal, value);
	}


	private void createSampleTracks(File tracksFolder, String... tracks) throws IOException {
		assertNotNull(tracksFolder);
		for (String track : tracks)
			my(IO.class).files().writeString(new File(tracksFolder, track), track);
	}


	private String playingTrack() {
		return _subject1.playingTrack().currentValue().name();
	}


	private void activateTrackEndorsementsFrom(Environment remoteEnvironment) throws IOException {
		Environments.runWith(remoteEnvironment, new ClosureX<IOException>() { @Override public void run() throws IOException {
			createSampleTracks(sharedTracksFolder(), new String[] { "track1.mp3", "track2.mp3", "track3.mp3" });
			assertEquals(3, sharedTracksFolder().listFiles().length);

			_subject2 = my(Music.class);
			_subject2.isTrackExchangeActive().setter().consume(true);

			my(CustomClockTicker.class).start(10, 2000);
		}});
	}


	private Collection<String> trackNames(File[] trackFiles) {
		return my(CollectionUtils.class).map(
			Arrays.asList(trackFiles),
			new Functor<File, String>() { @Override public String evaluate(File trackFile) throws RuntimeException {
				return trackFile.getName();
			}}
		);
	}


	private File sharedTracksFolder() {
		return my(Music.class).tracksFolder().currentValue();
	}

}
