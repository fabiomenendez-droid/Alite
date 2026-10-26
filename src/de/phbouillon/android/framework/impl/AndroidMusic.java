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

import java.io.FileInputStream;
import java.io.IOException;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import de.phbouillon.android.framework.Music;
import de.phbouillon.android.framework.Sound;
import de.phbouillon.android.games.alite.Settings;

public class AndroidMusic implements Music, OnCompletionListener, OnPreparedListener {
    private final MediaPlayer mediaPlayer = new MediaPlayer();
    private final Sound.SoundType soundType;
    private boolean prepared;
    private boolean playWhenReady;
    private boolean playbackAllowed = true;
    private boolean disposed;

    private void configure() {
        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            dispose();
            return true;
        });
    }

    public AndroidMusic(AndroidFileIO files, String path, Sound.SoundType type) throws IOException {
        soundType = type;
        configure();
        try (FileInputStream stream = new FileInputStream(files.getPrivatePath(path))) {
            mediaPlayer.setDataSource(stream.getFD());
            mediaPlayer.prepareAsync();
        } catch (IOException | RuntimeException e) {
            mediaPlayer.release();
            throw e;
        }
    }

    public AndroidMusic(AssetFileDescriptor descriptor, Sound.SoundType type, String fileName) throws IOException {
        soundType = type;
        configure();
        try {
            mediaPlayer.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
            mediaPlayer.prepareAsync();
        } catch (IOException | RuntimeException e) {
            mediaPlayer.release();
            throw e;
        }
    }

    @Override public synchronized void play() {
        if (disposed) return;
        playWhenReady = true;
        setVolume(Settings.volumes[soundType.getValue()]);
        if (prepared && playbackAllowed) mediaPlayer.start();
    }

    @Override public synchronized void stop() {
        if (disposed) return;
        playWhenReady = false;
        if (prepared) {
            mediaPlayer.pause();
            mediaPlayer.seekTo(0);
        }
    }

    @Override public synchronized void pause() {
        if (disposed) return;
        playWhenReady = false;
        if (prepared && mediaPlayer.isPlaying()) mediaPlayer.pause();
    }

    public synchronized void setPlaybackAllowed(boolean allowed) {
        if (disposed) return;
        playbackAllowed = allowed;
        if (!prepared) return;
        if (allowed && playWhenReady) mediaPlayer.start();
        else if (mediaPlayer.isPlaying()) mediaPlayer.pause();
    }

    @Override public synchronized void setLooping(boolean looping) {
        if (!disposed) mediaPlayer.setLooping(looping);
    }
    @Override public synchronized void setVolume(float volume) {
        if (!disposed) mediaPlayer.setVolume(volume, volume);
    }
    @Override public synchronized boolean isPlaying() {
        return !disposed && prepared && mediaPlayer.isPlaying();
    }
    @Override public synchronized boolean isStopped() { return disposed || !playWhenReady; }
    @Override public synchronized boolean isLooping() { return !disposed && mediaPlayer.isLooping(); }
    @Override public synchronized void dispose() {
        if (disposed) return;
        disposed = true;
        playWhenReady = false;
        mediaPlayer.release();
    }
    @Override public synchronized void onPrepared(MediaPlayer mp) {
        if (disposed) return;
        prepared = true;
        if (playWhenReady && playbackAllowed) mediaPlayer.start();
    }
    @Override public synchronized void onCompletion(MediaPlayer mp) { playWhenReady = false; }
}
