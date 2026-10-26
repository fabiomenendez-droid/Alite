package de.phbouillon.android.games.alite;

import android.content.Context;
import android.content.ContextWrapper;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import de.phbouillon.android.framework.impl.AndroidFileIO;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class StorageCompatibilityTest {
    private File internal;
    private File legacy;
    private Context context;

    @Before public void isolatedAppStorage() throws Exception {
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File root = new File(target.getCacheDir(), "storage-test-" + System.nanoTime());
        internal = new File(root, "internal");
        legacy = new File(root, "external");
        assertTrue(internal.mkdirs());
        assertTrue(legacy.mkdirs());
        context = new ContextWrapper(target) {
            @Override public File getFilesDir() { return internal; }
            @Override public File getExternalFilesDir(String type) { return legacy; }
        };
    }

    @Test public void legacyMigrationPreservesSettingsAndNestedCommandersOnce() throws Exception {
        File commander = new File(legacy, "commanders/test.cmdr");
        assertTrue(commander.getParentFile().mkdirs());
        try (OutputStream out = new FileOutputStream(commander)) { out.write(new byte[] {1,2,3}); }
        try (OutputStream out = new FileOutputStream(new File(legacy, ".alite"))) { out.write(42); }
        AndroidFileIO files = new AndroidFileIO(context);
        assertArrayEquals(new byte[] {1,2,3}, files.readFileContents("commanders/test.cmdr"));
        assertEquals(42, files.readFileContents(".alite")[0]);
        assertEquals(1, files.getFiles("commanders", ".*\\.cmdr").length);
        assertTrue(commander.exists());
        assertTrue(files.deleteFile("commanders/test.cmdr"));
        assertEquals(0, new AndroidFileIO(context).getFiles("commanders", ".*\\.cmdr").length);
    }

    @Test public void internalSaveReadsOffsetsAndRejectsTruncation() throws Exception {
        AndroidFileIO files = new AndroidFileIO(context);
        try (OutputStream out = files.writeFile("commanders/roundtrip.cmdr")) {
            out.write(new byte[] {10,20,30,40,50});
        }
        assertArrayEquals(new byte[] {30,40,50}, files.readFileContents("commanders/roundtrip.cmdr", 2));
        assertArrayEquals(new byte[] {20,30}, files.readPartialFileContents("commanders/roundtrip.cmdr", 1, 2));
        try {
            files.readPartialFileContents("commanders/roundtrip.cmdr", 10);
            fail("Truncated game state must raise EOFException");
        } catch (EOFException expected) { }
        assertTrue(files.getFile("commanders/roundtrip.cmdr").getPath().startsWith(internal.getCanonicalPath() + File.separator));
    }

    @Test public void pathAndZipTraversalCannotEscapeAppStorage() throws Exception {
        AndroidFileIO files = new AndroidFileIO(context);
        try { files.writeFile("../outside"); fail("Path traversal"); } catch (IOException expected) { }
        File archive = new File(internal, "invalid.zip");
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(archive))) {
            zip.putNextEntry(new ZipEntry("../outside"));
            zip.write(1);
        }
        try { files.unzip(archive, internal); fail("Zip traversal"); } catch (IOException expected) { }
    }

    @Test public void externalStorageCanBeUnavailable() throws Exception {
        Context noExternal = new ContextWrapper(context) {
            @Override public File getFilesDir() { return internal; }
            @Override public File getExternalFilesDir(String type) { return null; }
        };
        AndroidFileIO files = new AndroidFileIO(noExternal);
        try (OutputStream out = files.writeFile("commanders/internal.cmdr")) { out.write(7); }
        assertEquals(1, files.getFiles("commanders", ".*\\.cmdr").length);
    }
}
