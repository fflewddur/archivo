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
 * Model the status of an archive task.
 */
public class ArchiveStatus implements Comparable<ArchiveStatus> {
    private final TaskStatus status;
    private final double progress;
    private final int secondsRemaining;
    private final String message;

    public final static int TIME_UNKNOWN = -1;
    public final static ArchiveStatus EMPTY = new ArchiveStatus(TaskStatus.NONE);
    public final static ArchiveStatus QUEUED = new ArchiveStatus(TaskStatus.QUEUED);
    public final static ArchiveStatus REMUXING = new ArchiveStatus(TaskStatus.REMUXING);
    public final static ArchiveStatus FINDING_COMMERCIALS = new ArchiveStatus(TaskStatus.FINDING_COMMERCIALS);
    public final static ArchiveStatus REMOVING_COMMERCIALS = new ArchiveStatus(TaskStatus.REMOVING_COMMERCIALS);
    public final static ArchiveStatus FINISHED = new ArchiveStatus(TaskStatus.FINISHED);

    private ArchiveStatus(TaskStatus status) {
        this.status = status;
        progress = 0;
        secondsRemaining = 0;
        message = "";
    }

    private ArchiveStatus(TaskStatus status, double progress, int secondsRemaining) {
        this.status = status;
        this.progress = progress;
        this.secondsRemaining = secondsRemaining;
        this.message = "";
    }

    private ArchiveStatus(TaskStatus status, String message) {
        this.status = status;
        this.progress = 0;
        this.secondsRemaining = 0;
        this.message = message;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
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

    public static ArchiveStatus createErrorStatus(Throwable e) {
        return new ArchiveStatus(TaskStatus.ERROR, e.getLocalizedMessage());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ArchiveStatus)) {
            return false;
        } else if (obj == this) {
            return true;
        }

        ArchiveStatus other = (ArchiveStatus) obj;
        return (this.status == other.status && Double.compare(this.progress, other.progress) == 0 &&
                this.secondsRemaining == other.secondsRemaining && this.message.equals(other.message));
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = hash * 31 + status.ordinal();
        long bits = Double.doubleToLongBits(progress);
        hash = hash * 31 + (int)(bits ^ (bits >>> 32));
        hash = hash * 31 + secondsRemaining;
        hash = hash * 31 + message.hashCode();
        return hash;
    }

    @Override
    public int compareTo(ArchiveStatus o) {
        if (this.status != o.status) {
            return this.status.ordinal() - o.status.ordinal();
        } else if (Double.compare(this.progress, o.progress) != 0) {
            return Double.compare(this.progress, o.progress);
        } else if (!this.message.equals(o.message)){
            return this.message.compareTo(o.message);
        }
        return 0;
    }

    @Override
    public String toString() {
        return "ArchiveStatus{" +
                "status=" + status +
                ", progress=" + progress +
                ", secondsRemaining=" + secondsRemaining +
                ", message='" + message + '\'' +
                '}';
    }

    public enum TaskStatus {
        TRANSCODING,
        REMOVING_COMMERCIALS,
        FINDING_COMMERCIALS,
        REMUXING,
        DOWNLOADING,
        QUEUED,
        FINISHED,
        ERROR,
        NONE,;

        public boolean isCancelable() {
            switch (this) {
                case NONE:
                case FINISHED:
                case ERROR:
                    return false;
                case QUEUED:
                case DOWNLOADING:
                case TRANSCODING:
                case REMOVING_COMMERCIALS:
                case FINDING_COMMERCIALS:
                case REMUXING:
                    return true;
                default:
                    throw new AssertionError("Unknown TaskStatus");
            }
        }
    }
}
