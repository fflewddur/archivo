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

    public static ArchiveStatus EMPTY = new ArchiveStatus(TaskStatus.NONE, 0);
    public static ArchiveStatus QUEUED = new ArchiveStatus(TaskStatus.QUEUED, 0);
    public static ArchiveStatus FINISHED = new ArchiveStatus(TaskStatus.FINISHED, 0);

    private ArchiveStatus(TaskStatus status, double progress) {
        this.status = status;
        this.progress = progress;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public double getProgress() {
        return progress;
    }

    public static ArchiveStatus createDownloadingStatus(double progress) {
        return new ArchiveStatus(TaskStatus.DOWNLOADING, progress);
    }

    public static ArchiveStatus createTranscodingStatus(double progress) {
        return new ArchiveStatus(TaskStatus.TRANSCODING, progress);
    }

    public enum TaskStatus {
        NONE,
        QUEUED,
        DOWNLOADING,
        TRANSCODING,
        FINISHED
    }
}
