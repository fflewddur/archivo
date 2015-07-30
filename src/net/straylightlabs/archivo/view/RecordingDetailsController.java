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

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import net.straylightlabs.archivo.model.Recording;

import java.net.URL;
import java.util.ResourceBundle;

public class RecordingDetailsController implements Initializable {
    @FXML
    private Label title;

    @FXML
    private Label subtitle;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        clearRecording();
    }

    public void clearRecording() {
        title.setText("");
        subtitle.setText("");
    }

    public void showRecording(Recording recording) {
        if (recording == null || recording.isSeriesHeading()) {
            clearRecording();
        }

        title.setText(recording.getSeriesTitle());
        subtitle.setText(recording.getEpisodeTitle());
    }
}
