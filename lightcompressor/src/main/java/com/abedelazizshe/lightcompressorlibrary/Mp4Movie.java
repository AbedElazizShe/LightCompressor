/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */
package com.abedelazizshe.lightcompressorlibrary;

import android.media.MediaCodec;
import android.media.MediaFormat;

import com.googlecode.mp4parser.util.Matrix;

import java.io.File;
import java.util.ArrayList;

public class Mp4Movie {
    private Matrix matrix = Matrix.ROTATE_0;
    private final ArrayList<Track> tracks = new ArrayList<>();
    private File cacheFile;

    Matrix getMatrix() {
        return matrix;
    }

    void setCacheFile(File file) {
        cacheFile = file;
    }

    void setRotation(int angle) {
        if (angle == 0) {
            matrix = Matrix.ROTATE_0;
        } else if (angle == 90) {
            matrix = Matrix.ROTATE_90;
        } else if (angle == 180) {
            matrix = Matrix.ROTATE_180;
        } else if (angle == 270) {
            matrix = Matrix.ROTATE_270;
        }
    }

    ArrayList<Track> getTracks() {
        return tracks;
    }

    File getCacheFile() {
        return cacheFile;
    }

    void addSample(int trackIndex, long offset, MediaCodec.BufferInfo bufferInfo) {
        if (trackIndex < 0 || trackIndex >= tracks.size()) {
            return;
        }
        Track track = tracks.get(trackIndex);
        track.addSample(offset, bufferInfo);
    }

    int addTrack(MediaFormat mediaFormat, boolean isAudio) {
        tracks.add(new Track(tracks.size(), mediaFormat, isAudio));
        return tracks.size() - 1;
    }
}
