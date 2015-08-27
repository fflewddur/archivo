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
//            progressIndicator.getStyleClass().add("recording-progress-indicator");
        }
        return progressIndicator;
    }

    @Override
    protected void updateItem(ArchiveStatus status, boolean isEmpty) {
        super.updateItem(status, isEmpty);

        if (status != null && status.getStatus() != ArchiveStatus.TaskStatus.NONE) {
            ProgressIndicator indicator;
            switch (status.getStatus()) {
                case QUEUED:
                    setText("Queued...");
                    setGraphic(null);
                    break;
                case DOWNLOADING:
                    setText("Downloading...");
                    indicator = getProgressIndicator();
                    indicator.setProgress(status.getProgress());
                    setGraphic(indicator);
                    break;
                case TRANSCODING:
                    setText("Transcoding...");
                    indicator = getProgressIndicator();
                    indicator.setProgress(status.getProgress());
                    setGraphic(indicator);
                    break;
                case FINISHED:
                    setText("Finished!");
                    setGraphic(null);
                    break;
                default:
                    setText(status.getStatus().toString());
                    setGraphic(null);
                    break;
            }
        } else {
            setText(null);
            setGraphic(null);
            setStyle("");
        }
    }
}
