package de.phbouillon.android.games.alite;

import android.app.Instrumentation;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.opengl.GLSurfaceView;
import android.os.SystemClock;
import android.view.MotionEvent;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import de.phbouillon.android.framework.Screen;
import de.phbouillon.android.games.alite.screens.canvas.*;
import de.phbouillon.android.games.alite.screens.opengl.ingame.FlightScreen;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** A real Android 15 activity/surface/input/save test, not a gameplay certification. */
@RunWith(AndroidJUnit4.class)
public class Android15GameTest {
    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();

    private void onGl(Alite game, Runnable action) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        ((GLSurfaceView) game.getCurrentView()).queueEvent(() -> {
            try { action.run(); } catch (Throwable t) { error.set(t); } finally { done.countDown(); }
        });
        assertTrue("GL thread timeout", done.await(60, TimeUnit.SECONDS));
        if (error.get() != null) throw new AssertionError("GL action failed", error.get());
    }

    private void awaitScreen(Alite game, Class<?> expected) {
        long until = SystemClock.uptimeMillis() + 60000;
        while (SystemClock.uptimeMillis() < until) {
            Screen screen = game.getCurrentScreen();
            assertFalse("Game entered FatalExceptionScreen", screen instanceof FatalExceptionScreen);
            if (expected.isInstance(screen)) return;
            SystemClock.sleep(100);
        }
        fail("Expected " + expected.getSimpleName() + ", got " + game.getCurrentScreen());
    }

    private void tap(Alite game, int x, int y) {
        AtomicReference<float[]> point = new AtomicReference<>();
        instrumentation.runOnMainSync(() -> {
            int[] location = new int[2];
            game.getCurrentView().getLocationOnScreen(location);
            Rect area = game.calculateTargetRect(new Rect(0,0,game.getSize()[0],game.getSize()[1]));
            point.set(new float[] {location[0]+area.left+x*area.width()/1920f,
                location[1]+area.top+y*area.height()/1080f});
        });
        long time = SystemClock.uptimeMillis();
        float[] p = point.get();
        MotionEvent down = MotionEvent.obtain(time,time,MotionEvent.ACTION_DOWN,p[0],p[1],0);
        MotionEvent up = MotionEvent.obtain(time,time+80,MotionEvent.ACTION_UP,p[0],p[1],0);
        instrumentation.sendPointerSync(down);
        instrumentation.sendPointerSync(up);
        down.recycle(); up.recycle();
    }

    private void assertNoRenderFailure(Alite game) throws Exception {
        onGl(game, () -> {
            try {
                java.lang.reflect.Field field = de.phbouillon.android.framework.impl.AndroidGame.class.getDeclaredField("fatalException");
                field.setAccessible(true);
                assertNull("Renderer entered the fatal-error screen", field.get(game));
            } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        });
    }

    private void screenshot(Alite game, String name) throws Exception {
        // Let at least several complete frames reach SurfaceFlinger.
        SystemClock.sleep(1500);
        assertNoRenderFailure(game);
        Bitmap image = instrumentation.getUiAutomation().takeScreenshot();
        assertNotNull(image);
        HashSet<Integer> colors = new HashSet<>();
        for (int y=0; y<image.getHeight(); y+=16) {
            for (int x=0; x<image.getWidth(); x+=16) colors.add(image.getPixel(x,y));
        }
        assertTrue("Blank/monochrome output: " + colors.size(), colors.size() > 16);
        File directory = new File(game.getFilesDir(), "test-evidence");
        directory.mkdirs();
        try (FileOutputStream out = new FileOutputStream(new File(directory, name+".png"))) {
            assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, out));
        }
        image.recycle();
    }

    @Test public void launchTouchTradeGalaxyFlightSaveAndResume() throws Exception {
        try (ActivityScenario<Alite> scenario = ActivityScenario.launch(Alite.class)) {
            AtomicReference<Alite> current = new AtomicReference<>();
            scenario.onActivity(current::set);
            final Alite game = current.get();
            awaitScreen(game, ShipIntroScreen.class);
            screenshot(game,"ship-intro");
            tap(game,960,1010);
            awaitScreen(game, StatusScreen.class);
            screenshot(game,"status");
            onGl(game, () -> {
                try {
                    String name = game.getPlayer().getName();
                    long cash = game.getPlayer().getCash();
                    game.getFileUtils().saveCommander(game, "Android15Test", "commanders/android15-test.cmdr");
                    game.getPlayer().setCash(123456);
                    game.getFileUtils().loadCommander(game, "commanders/android15-test.cmdr");
                    assertEquals("Android15Test", game.getPlayer().getName());
                    assertEquals(cash, game.getPlayer().getCash());
                    game.getPlayer().setName(name);
                    game.getFileIO().deleteFile("commanders/android15-test.cmdr");
                } catch (Exception e) { throw new RuntimeException(e); }
            });
            onGl(game, () -> game.setScreen(new BuyScreen(game)));
            awaitScreen(game,BuyScreen.class);
            screenshot(game,"trade");
            onGl(game, () -> game.setScreen(new GalaxyScreen(game)));
            awaitScreen(game,GalaxyScreen.class);
            screenshot(game,"galaxy");
            onGl(game, () -> game.setScreen(new FlightScreen(game,true)));
            awaitScreen(game,FlightScreen.class);
            screenshot(game,"flight");
            scenario.moveToState(Lifecycle.State.CREATED);
            assertTrue(game.getFileIO().exists(AliteStartManager.ALITE_STATE_FILE));
            scenario.moveToState(Lifecycle.State.RESUMED);
            scenario.onActivity(current::set);
            assertSame(game, current.get());
            awaitScreen(game,FlightScreen.class);
            screenshot(game,"resumed-flight");
        }
    }
}
