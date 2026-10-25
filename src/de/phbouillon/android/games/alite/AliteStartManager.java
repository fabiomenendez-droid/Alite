package de.phbouillon.android.games.alite;

/* Alite - Discover the Universe on your Favorite Android Device
 * Copyright (C) 2015 Philipp Bouillon
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful and
 * fun, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see
 * http://http://www.gnu.org/licenses/gpl-3.0.txt.
 */

import java.io.IOException;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import de.phbouillon.android.framework.impl.AndroidFileIO;

/** Offline launcher: every game asset is installed with the APK. */
public class AliteStartManager extends Activity {
    public static final int ALITE_RESULT_CLOSE_ALL = 78615265;
    public static final String ALITE_STATE_FILE = "current_state.dat";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidFileIO fileIO = new AndroidFileIO(this);
        if (!AliteLog.isInitialized()) {
            AliteLog.initialize(fileIO);
        }
        Settings.load(fileIO);
        if (savedInstanceState != null) {
            return;
        }
        boolean hasState = false;
        try {
            hasState = fileIO.exists(ALITE_STATE_FILE);
        } catch (IOException e) {
            AliteLog.e("Launcher", "Cannot inspect saved state", e);
        }
        Intent intent = new Intent(this, hasState ? Alite.class : AliteIntro.class);
        intent.putExtra(Alite.LOG_IS_INITIALIZED, true);
        startActivityForResult(intent, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        finish();
    }
}
