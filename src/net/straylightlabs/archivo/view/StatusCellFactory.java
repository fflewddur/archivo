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

package net.straylightlabs.archivo.view;

import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeTableCell;
import net.straylightlabs.archivo.model.ArchiveStatus;
import net.straylightlabs.archivo.model.Recording;

public class StatusCellFactory extends TreeTableCell<Recording, ArchiveStatus> {
    private ProgressIndicator progressIndicator;

    public StatusCellFactory() {
        super();
    }

    private ProgressIndicator getProgressIndicator() {
        if (progressIndicator == null) {
            progressIndicator = new ProgressIndicator();
            progressIndicator.setPrefHeight(20);
            progressIndicator.setPrefWidth(20);
            progressIndicator.setSkin(new TaskProgressIndicatorSkin(progressIndicator));
            progressIndicator.getStyleClass().add("recording-progress-indicator");
        }
        return progressIndicator;
    }

    @Override
    protected void updateItem(ArchiveStatus status, boolean isEmpty) {
        super.updateItem(status, isEmpty);

        Recording recording = null;
        if (getTreeTableRow() != null && getTreeTableRow().getTreeItem() != null) {
            recording = getTreeTableRow().getTreeItem().getValue();
        }
        if (status != null && status.getStatus() != ArchiveStatus.TaskStatus.NONE && recording != null && !recording.isSeriesHeading()) {
            switch (status.getStatus()) {
                case QUEUED:
                    setText("Queued...");
                    setGraphic(null);
                    updateTooltip(null);
                    break;
                case DOWNLOADING:
                    setText(String.format("Downloading... (%s)", formatTime(status.getSecondsRemaining())));
                    setProgress(status.getProgress());
                    updateTooltip(String.format("Download speed: %s", formatSpeed(status.getKilobytesPerSecond())));
                    break;
                case REMUXING:
                    setText(String.format("Repairing video file... (%s)", formatTime(status.getSecondsRemaining())));
                    setProgress(status.getProgress());
                    updateTooltip(null);
                    break;
                case FINDING_COMMERCIALS:
                    setText(String.format("Finding commercials... (%s)", formatTime(status.getSecondsRemaining())));
                    setProgress(status.getProgress());
                    updateTooltip(null);
                    break;
                case REMOVING_COMMERCIALS:
                    setText(String.format("Removing commercials... (%s)", formatTime(status.getSecondsRemaining())));
                    setProgress(status.getProgress());
                    updateTooltip(null);
                    break;
                case TRANSCODING:
                    setText(String.format("Compressing video... (%s)", formatTime(status.getSecondsRemaining())));
                    setProgress(status.getProgress());
                    updateTooltip(null);
                    break;
                case FINISHED:
                    setText("Archived");
                    setProgress(1.0);
                    updateTooltip(null);
                    break;
                case ERROR:
                    setText(status.getMessage());
                    setGraphic(null);
                    updateTooltip(null);
                    break;
                default:
                    setText(status.getStatus().toString());
                    setGraphic(null);
                    updateTooltip(null);
                    break;
            }
        } else {
            setText(null);
            setGraphic(null);
            updateTooltip(null);
            setStyle("");
        }
    }

    private void updateTooltip(String tip) {
        if (tip == null) {
            setTooltip(null);
        } else {
            Tooltip tooltip = getTooltip();
            if (tooltip == null) {
                tooltip = new Tooltip();
                tooltip.setText(tip);
                setTooltip(tooltip);
            } else {
                tooltip.setText(tip);
            }
        }
    }

    private void setProgress(double value) {
        ProgressIndicator indicator = getProgressIndicator();
        indicator.setProgress(value);
        setGraphic(indicator);
    }

    private String formatTime(int seconds) {
        if (seconds == ArchiveStatus.TIME_UNKNOWN) {
            return "calculating time left";
        } else if (seconds <= 30) {
            return "about 30 seconds left";
        } else if (seconds <= 65) {
            return "about 1 minute left";
        } else if (seconds < (60 * 60)) {
            return "" + (seconds / 60 + 1) + " minutes left";
        } else {
            int hours = seconds / 60 / 60;
            if (hours > 1) {
                return String.format("more than %d hours left", hours);
            } else {
                return "more than 1 hour left";
            }
        }
    }

    private String formatSpeed(double kbs) {
        String speed;
        if (kbs >= 1024) {
            speed = String.format("%,.1f MB/s", kbs / 1024);
        } else if (kbs < 10) {
            speed = String.format("%,.1f KB/s", kbs);
        } else {
            speed = String.format("%,.0f KB/S", kbs);
        }
        return speed;
    }
}
