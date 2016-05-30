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

package net.straylightlabs.archivo.view;

import javafx.scene.Node;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableRow;
import net.straylightlabs.archivo.model.ArchiveStatus;
import net.straylightlabs.archivo.model.Recording;

class StatusCellFactory extends TreeTableCell<Recording, ArchiveStatus> {
    private ProgressIndicator progressIndicator;

    private static final String STYLE_FINISHED = "status-finished";
    private static final String STYLE_UNAVAILABLE = "status-unavailable";

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
        TreeTableRow<Recording> row = getTreeTableRow();
        if (row != null && row.getTreeItem() != null) {
            recording = row.getTreeItem().getValue();
            clearCustomClasses(row);
            if (!recording.isSeriesHeading() && (recording.isCopyProtected() || recording.isInProgress())) {
                row.getStyleClass().add(STYLE_UNAVAILABLE);
            }
        }
        if (status != null && status.getStatus() != ArchiveStatus.TaskStatus.NONE && recording != null && !recording.isSeriesHeading()) {

            switch (status.getStatus()) {
                case QUEUED:
                    setText("Waiting to download...");
                    setGraphic(null);
                    updateTooltip(null);
                    break;
                case CONNECTING:
                    setProgress(ArchiveStatus.INDETERMINATE);
                    int secondsRemaining = status.getSecondsRemaining();
                    if (secondsRemaining > 0) {
                        setText(String.format("Connection failed, retrying in %s...", formatRetryTime(secondsRemaining)));
                        int retriesRemaining = status.getRetriesRemaining();
                        String msg = String.format("Your TiVo is too busy to connect to right now. We'll try to reconnect %d more time", retriesRemaining);
                        if (retriesRemaining != 1) {
                            msg += "s";
                        }
                        msg += ".";
                        updateTooltip(msg);
                    } else {
                        setText("Connecting...");
                        updateTooltip(null);
                    }
                    break;
                case DOWNLOADING:
                    setText(String.format("Downloading... (%s)", formatRemainingTime(status.getSecondsRemaining())));
                    setProgress(status.getProgress());
                    updateTooltip(String.format("Download speed: %s", formatSpeed(status.getKilobytesPerSecond())));
                    break;
                case DOWNLOADED:
                    setText("Download ready, waiting to process...");
                    setProgress(ArchiveStatus.INDETERMINATE);
                    updateTooltip("We'll process this recording after the current archive has finished");
                    break;
                case REMUXING:
                    setText(String.format("Repairing video file... (%s)", formatRemainingTime(status.getSecondsRemaining())));
                    setProgress(status.getProgress());
                    updateTooltip(null);
                    break;
                case FINDING_COMMERCIALS:
                    setText(String.format("Finding commercials... (%s)", formatRemainingTime(status.getSecondsRemaining())));
                    setProgress(status.getProgress());
                    updateTooltip(null);
                    break;
                case REMOVING_COMMERCIALS:
                    setText(String.format("Removing commercials... (%s)", formatRemainingTime(status.getSecondsRemaining())));
                    setProgress(status.getProgress());
                    updateTooltip(null);
                    break;
                case TRANSCODING:
                    setText(String.format("Compressing video... (%s)", formatRemainingTime(status.getSecondsRemaining())));
                    setProgress(status.getProgress());
                    updateTooltip(null);
                    break;
                case FINISHED:
                    setText("Archived");
                    setProgress(1.0);
                    row.getStyleClass().add(STYLE_FINISHED);
                    updateTooltip(null);
                    break;
                case ERROR:
                    setText(status.getMessage());
                    setGraphic(null);
                    updateTooltip(status.getTooltip());
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

    private void clearCustomClasses(Node node) {
        node.getStyleClass().removeAll(STYLE_FINISHED, STYLE_UNAVAILABLE);
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

    private String formatRetryTime(int seconds) {
        if (seconds < 60) {
            return String.format("%d seconds", seconds);
        } else {
            int minutes = (int) Math.round(seconds / 60.0);
            if (minutes == 1) {
                return String.format("%d minute", minutes);
            } else {
                return String.format("%d minutes", minutes);
            }
        }
    }

    private String formatRemainingTime(int seconds) {
        if (seconds == ArchiveStatus.TIME_UNKNOWN) {
            return "calculating time left";
        } else if (seconds <= 5) {
            return ("about 10 seconds left");
        } else if (seconds <= 20) {
            return ("about 20 seconds left");
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
