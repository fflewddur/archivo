/*
 * Copyright 2015-2016 Todd Kulesza <todd@dropline.net>.
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

import net.straylightlabs.archivo.controller.ArchiveTaskException;

/**
 * Model the status of an archive task.
 */
public class ArchiveStatus implements Comparable<ArchiveStatus> {
    private final TaskStatus status;
    private final double progress;
    private final int secondsRemaining;
    private final int retriesUsed;
    private final int retriesRemaining;
    private final double kbs; // KB per sec
    private final String message;
    private final String tooltip;

    public final static int TIME_UNKNOWN = -1;
    public final static int INDETERMINATE = -1;
    public final static ArchiveStatus EMPTY = new ArchiveStatus(TaskStatus.NONE);
    public final static ArchiveStatus QUEUED = new ArchiveStatus(TaskStatus.QUEUED);
    public final static ArchiveStatus FINISHED = new ArchiveStatus(TaskStatus.FINISHED);
    public final static ArchiveStatus DOWNLOADED = new ArchiveStatus(TaskStatus.DOWNLOADED);

    private ArchiveStatus(TaskStatus status) {
        this.status = status;
        progress = 0;
        secondsRemaining = 0;
        kbs = 0;
        retriesUsed = 0;
        retriesRemaining = 0;
        message = "";
        tooltip = "";
    }

    private ArchiveStatus(TaskStatus status, int retriesUsed, int retriesRemaining, int secondsRemaining) {
        this.status = status;
        progress = 0;
        this.secondsRemaining = secondsRemaining;
        kbs = 0;
        this.retriesUsed = retriesUsed;
        this.retriesRemaining = retriesRemaining;
        message = "";
        tooltip = "";
    }

    private ArchiveStatus(TaskStatus status, double progress, int secondsRemaining) {
        this.status = status;
        this.progress = progress;
        this.secondsRemaining = secondsRemaining;
        kbs = 0;
        retriesUsed = 0;
        retriesRemaining = 0;
        message = "";
        tooltip = "";
    }

    private ArchiveStatus(TaskStatus status, double progress, int secondsRemaining, double kbs) {
        this.status = status;
        this.progress = progress;
        this.secondsRemaining = secondsRemaining;
        this.kbs = kbs;
        retriesUsed = 0;
        retriesRemaining = 0;
        message = "";
        tooltip = "";
    }

    private ArchiveStatus(TaskStatus status, String message) {
        this.status = status;
        progress = 0;
        secondsRemaining = 0;
        kbs = 0;
        retriesUsed = 0;
        retriesRemaining = 0;
        this.message = message;
        tooltip = null;
    }

    private ArchiveStatus(TaskStatus status, String message, String tooltip) {
        this.status = status;
        progress = 0;
        secondsRemaining = 0;
        kbs = 0;
        retriesUsed = 0;
        retriesRemaining = 0;
        this.message = message;
        this.tooltip = tooltip;
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

    public double getKilobytesPerSecond() {
        return kbs;
    }

    @SuppressWarnings("unused")
    public int getRetriesUsed() {
        return retriesUsed;
    }

    public int getRetriesRemaining() {
        return retriesRemaining;
    }

    public String getTooltip() {
        return tooltip;
    }

    public static ArchiveStatus createConnectingStatus(int secondsToWait, int failures, int retriesRemaining) {
        return new ArchiveStatus(TaskStatus.CONNECTING, failures, retriesRemaining, secondsToWait);
    }

    public static ArchiveStatus createDownloadingStatus(double progress, int secondsRemaining, double kbs) {
        if (progress >= 1.0) {
            progress = .99;
        }
        return new ArchiveStatus(TaskStatus.DOWNLOADING, progress, secondsRemaining, kbs);
    }

    public static ArchiveStatus createRemuxingStatus(double progress, int secondsRemaining) {
        if (progress >= 1.0) {
            progress = .99;
        }
        return new ArchiveStatus(TaskStatus.REMUXING, progress, secondsRemaining);
    }

    public static ArchiveStatus createFindingCommercialsStatus(double progress, int secondsRemaining) {
        if (progress >= 1.0) {
            progress = .99;
        }
        return new ArchiveStatus(TaskStatus.FINDING_COMMERCIALS, progress, secondsRemaining);
    }

    public static ArchiveStatus createRemovingCommercialsStatus(double progress, int secondsRemaining) {
        if (progress >= 1.0) {
            progress = .99;
        }
        return new ArchiveStatus(TaskStatus.REMOVING_COMMERCIALS, progress, secondsRemaining);
    }

    public static ArchiveStatus createTranscodingStatus(double progress, int secondsRemaining) {
        if (progress >= 1.0) {
            progress = .99;
        }
        return new ArchiveStatus(TaskStatus.TRANSCODING, progress, secondsRemaining);
    }

    public static ArchiveStatus createErrorStatus(Throwable e) {
        if (e instanceof ArchiveTaskException) {
            ArchiveTaskException ate = (ArchiveTaskException) e;
            return new ArchiveStatus(TaskStatus.ERROR, ate.getLocalizedMessage(), ate.getTooltip());
        } else {
            return new ArchiveStatus(TaskStatus.ERROR, e.getLocalizedMessage());
        }
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
        hash = hash * 31 + (int) (bits ^ (bits >>> 32));
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
        } else if (!this.message.equals(o.message)) {
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
        CONNECTING,
        DOWNLOADING,
        DOWNLOADED,
        QUEUED,
        FINISHED,
        ERROR,
        NONE;

        public boolean isCancelable() {
            switch (this) {
                case NONE:
                case FINISHED:
                case ERROR:
                    return false;
                case QUEUED:
                case CONNECTING:
                case DOWNLOADING:
                case DOWNLOADED:
                case TRANSCODING:
                case REMOVING_COMMERCIALS:
                case FINDING_COMMERCIALS:
                case REMUXING:
                    return true;
                default:
                    throw new AssertionError("Unknown TaskStatus");
            }
        }

        public boolean isRemovable() {
            switch (this) {
                case NONE:
                case FINISHED:
                case ERROR:
                    return true;
                case QUEUED:
                case CONNECTING:
                case DOWNLOADING:
                case DOWNLOADED:
                case TRANSCODING:
                case REMOVING_COMMERCIALS:
                case FINDING_COMMERCIALS:
                case REMUXING:
                    return false;
                default:
                    throw new AssertionError("Unknown TaskStatus");
            }
        }
    }
}
