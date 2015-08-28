/*
 * Copyright 2015 Todd Kulesza <todd@dropline.net>.
 *
 * This file is part of Archivo.
 *
 * Archivo is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Archivo is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Archivo.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.straylightlabs.archivo.model;

/**
 * Created by todd on 8/26/15.
 */
public class ArchiveStatus {
    private final TaskStatus status;
    private final double progress;
    private final int secondsRemaining;

    public final static int TIME_UNKNOWN = -1;
    public final static ArchiveStatus EMPTY = new ArchiveStatus(TaskStatus.NONE);
    public final static ArchiveStatus QUEUED = new ArchiveStatus(TaskStatus.QUEUED);
    public final static ArchiveStatus FINISHED = new ArchiveStatus(TaskStatus.FINISHED);
    public final static ArchiveStatus ERROR = new ArchiveStatus(TaskStatus.ERROR);

    private ArchiveStatus(TaskStatus status) {
        this.status = status;
        progress = 0;
        secondsRemaining = 0;
    }

    private ArchiveStatus(TaskStatus status, double progress, int secondsRemaining) {
        this.status = status;
        this.progress = progress;
        this.secondsRemaining = secondsRemaining;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public double getProgress() {
        return progress;
    }

    public int getSecondsRemaining() {
        return secondsRemaining;
    }

    public static ArchiveStatus createDownloadingStatus(double progress, int secondsRemaining) {
        return new ArchiveStatus(TaskStatus.DOWNLOADING, progress, secondsRemaining);
    }

    public static ArchiveStatus createTranscodingStatus(double progress, int secondsRemaining) {
        return new ArchiveStatus(TaskStatus.TRANSCODING, progress, secondsRemaining);
    }

    public enum TaskStatus {
        NONE,
        QUEUED,
        DOWNLOADING,
        TRANSCODING,
        FINISHED,
        ERROR
    }
}
