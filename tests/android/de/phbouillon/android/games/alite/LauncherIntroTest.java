package de.phbouillon.android.games.alite;

import android.app.Instrumentation;
import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import de.phbouillon.android.framework.impl.VideoView;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class LauncherIntroTest {
    @Test public void offlineLauncherPlaysIntroAndCanSkipToGame() throws Exception {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor introMonitor = instrumentation.addMonitor(AliteIntro.class.getName(), null, false);
        Instrumentation.ActivityMonitor gameMonitor = instrumentation.addMonitor(Alite.class.getName(), null, false);
        try {
            Intent intent = new Intent(instrumentation.getTargetContext(), AliteStartManager.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            instrumentation.startActivitySync(intent);
            AliteIntro intro = (AliteIntro) introMonitor.waitForActivityWithTimeout(60000);
            assertNotNull("Offline launcher did not reach intro", intro);
            AtomicReference<VideoView> video = new AtomicReference<>();
            AtomicReference<Boolean> playing = new AtomicReference<>(false);
            long until = SystemClock.uptimeMillis() + 60000;
            while (!playing.get() && SystemClock.uptimeMillis() < until) {
                instrumentation.runOnMainSync(() -> {
                    ViewGroup container = intro.findViewById(R.id.introContainer);
                    for (int i=0; i<container.getChildCount(); i++) {
                        View child = container.getChildAt(i);
                        if (child instanceof VideoView) {
                            video.set((VideoView)child);
                            playing.set(video.get().isPlaying());
                        }
                    }
                });
                SystemClock.sleep(100);
            }
            assertTrue("Bundled intro video did not play", playing.get());
            instrumentation.runOnMainSync(() -> intro.onClick(video.get()));
            Alite game = (Alite) gameMonitor.waitForActivityWithTimeout(60000);
            assertNotNull("Skipping intro did not start the game", game);
            instrumentation.runOnMainSync(() -> { game.finish(); intro.finish(); });
        } finally {
            instrumentation.removeMonitor(introMonitor);
            instrumentation.removeMonitor(gameMonitor);
        }
    }
}
