package de.phbouillon.android.games.alite;

import android.app.Instrumentation;
import android.content.res.AssetFileDescriptor;
import android.os.SystemClock;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import de.phbouillon.android.framework.Sound;
import de.phbouillon.android.framework.impl.AndroidMusic;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Exercises the original bundled MP3 independently of the GLES renderer. */
@RunWith(AndroidJUnit4.class)
public class AudioCompatibilityTest {
    private void awaitPlaying(AndroidMusic music) {
        long until = SystemClock.uptimeMillis() + 20000;
        while (!music.isPlaying() && SystemClock.uptimeMillis() < until) SystemClock.sleep(50);
        assertTrue("Original music did not prepare/start", music.isPlaying());
    }

    @Test public void bundledMusicCanPauseRestartAndRelease() throws Exception {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        AtomicReference<AndroidMusic> current = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        instrumentation.runOnMainSync(() -> {
            try (AssetFileDescriptor descriptor = instrumentation.getTargetContext().getAssets()
                    .openFd("music/the_chase.mp3")) {
                current.set(new AndroidMusic(descriptor, Sound.SoundType.MUSIC, "music/the_chase.mp3"));
                current.get().play();
            } catch (Throwable t) { error.set(t); }
        });
        if (error.get() != null) throw new AssertionError("Music initialization", error.get());
        AndroidMusic music = current.get();
        try {
            awaitPlaying(music);
            music.setPlaybackAllowed(false);
            assertFalse(music.isPlaying());
            music.setPlaybackAllowed(true);
            awaitPlaying(music);
            music.pause();
            assertFalse(music.isPlaying());
            music.play();
            awaitPlaying(music);
            music.stop();
            assertTrue(music.isStopped());
            music.play();
            awaitPlaying(music);
        } finally {
            music.dispose();
            music.dispose();
        }
        assertFalse(music.isPlaying());
    }
}
