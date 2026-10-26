package de.phbouillon.android.framework.impl;

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
import java.util.WeakHashMap;
import android.content.Context;
import android.os.Build;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.SoundPool;
import de.phbouillon.android.framework.Audio;
import de.phbouillon.android.framework.Music;
import de.phbouillon.android.framework.Sound;
import de.phbouillon.android.games.alite.AliteConfig;
import de.phbouillon.android.games.alite.AliteLog;

public class AndroidAudio implements Audio {
	public static final int MAXIMUM_NUMBER_OF_CONCURRENT_SAMPLES = 20;
	
	private final AssetManager assets;
	private final SoundPool soundPool;
	private final AndroidFileIO fileIO;
	private final AudioManager audioManager;
	private final AudioAttributes attributes;
	private final AudioManager.OnAudioFocusChangeListener focusListener;
	private AudioFocusRequest focusRequest;
	private final WeakHashMap<AndroidMusic, Boolean> music = new WeakHashMap<>();
	private boolean foreground;
	private boolean audioAllowed;
	private boolean released;
	
	public AndroidAudio(Activity activity, AndroidFileIO fileIO) {
		activity.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		this.assets = activity.getAssets();
		this.fileIO = fileIO;
		attributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
			.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
		this.soundPool = new SoundPool.Builder().setMaxStreams(MAXIMUM_NUMBER_OF_CONCURRENT_SAMPLES)
			.setAudioAttributes(attributes).build();
		audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
		focusListener = change -> setAudioAllowed(foreground && change == AudioManager.AUDIOFOCUS_GAIN);
		if (Build.VERSION.SDK_INT >= 26) {
			focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
				.setAudioAttributes(attributes).setOnAudioFocusChangeListener(focusListener)
				.setWillPauseWhenDucked(true).build();
		}
	}
	
	@Override
	public synchronized Music newMusic(String fileName, Sound.SoundType soundType) {
		if (released) return null;
		if (AliteConfig.HAS_EXTENSION_APK) {
			try {
				return track(new AndroidMusic(fileIO, fileName, soundType));
			} catch (IOException e) {
				AliteLog.e("Cannot load music", "Music " + fileName + " not found.");
				return null;
			}			
		} else {
			try {
				AliteLog.d("Loading Music", "Loading music " + fileName + ", Type:" + soundType);
				try (AssetFileDescriptor assetDescriptor = assets.openFd(fileName)) {
					return track(new AndroidMusic(assetDescriptor, soundType, fileName));
				}
			} catch (IOException e) {
				AliteLog.e("Cannot load music", "Music " + fileName + " not found.");
				return null;
			}
		}
	}

	@Override
	public synchronized Sound newSound(String fileName, Sound.SoundType soundType) {
		if (released) return null;
		try {		
			AliteLog.d("Loading Sound", "Loading sound " + fileName);
			int soundId;
			if (AliteConfig.HAS_EXTENSION_APK) {
				soundId = soundPool.load(fileIO.getPrivatePath(fileName), 0);
			} else {
				try (AssetFileDescriptor descriptor = fileIO.getFileDescriptor(fileName)) {
					soundId = soundPool.load(descriptor, 0);
				}
			}
			return new AndroidSound(soundPool, soundId, soundType, this);
		} catch (IOException e) {
			AliteLog.e("Cannot load sound", "Sound " + fileName + " not found.");
			return null;			
		}
	}	

	private AndroidMusic track(AndroidMusic item) {
		item.setPlaybackAllowed(audioAllowed);
		music.put(item, Boolean.TRUE);
		return item;
	}

	public synchronized boolean isPlaybackAllowed() { return audioAllowed && !released; }

	private synchronized void setAudioAllowed(boolean allowed) {
		if (released) return;
		audioAllowed = allowed;
		for (AndroidMusic item : music.keySet()) item.setPlaybackAllowed(allowed);
		if (allowed) soundPool.autoResume(); else soundPool.autoPause();
	}

	public synchronized void resume() {
		if (released) return;
		foreground = true;
		int result = Build.VERSION.SDK_INT >= 26
			? audioManager.requestAudioFocus(focusRequest)
			: audioManager.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
		setAudioAllowed(result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
	}

	public synchronized void pause() {
		foreground = false;
		setAudioAllowed(false);
		if (Build.VERSION.SDK_INT >= 26) audioManager.abandonAudioFocusRequest(focusRequest);
		else audioManager.abandonAudioFocus(focusListener);
	}

	public synchronized void dispose() {
		if (released) return;
		pause();
		for (AndroidMusic item : music.keySet()) item.dispose();
		music.clear();
		soundPool.release();
		released = true;
	}
}
