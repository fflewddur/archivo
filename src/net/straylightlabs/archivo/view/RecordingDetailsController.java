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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import net.straylightlabs.archivo.model.Recording;

import java.net.URL;
import java.util.ResourceBundle;

public class RecordingDetailsController implements Initializable {
    @FXML
    private Label title;
    @FXML
    private Label subtitle;
    @FXML
    private Label episode;
    @FXML
    private Label originalAirDate;
    @FXML
    private Label date;
    @FXML
    private Label channel;
    @FXML
    private Label duration;
    @FXML
    private Label description;
    @FXML
    private ImageView image;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        clearRecording();
        image.setFitWidth(Recording.DESIRED_IMAGE_WIDTH);
        image.setFitHeight(Recording.DESIRED_IMAGE_HEIGHT);
    }

    public void clearRecording() {
        setLabelText(title, "");
        setLabelText(subtitle, "");
        setLabelText(episode, "");
        setLabelText(date, "");
        setLabelText(originalAirDate, "");
        setLabelText(channel, "");
        setLabelText(duration, "");
        setLabelText(description, "");
        image.setImage(null);
    }

    public void showRecording(Recording recording) {
        if (recording == null) {
            clearRecording();
        } else if (recording.isSeriesHeading()) {
            showRecordingOverview(recording);
        } else {
            showRecordingDetails(recording);
        }
    }

    private void showRecordingOverview(Recording recording) {
        clearRecording();

        setLabelText(title, recording.getSeriesTitle());
        if (recording.getNumEpisodes() == 1) {
            setLabelText(subtitle, String.format("%d episode", recording.getNumEpisodes()));
        } else {
            setLabelText(subtitle, String.format("%d episodes", recording.getNumEpisodes()));
        }

        if (recording.getImageURL() != null)
            image.setImage(new Image(recording.getImageURL().toString(),
                    Recording.DESIRED_IMAGE_WIDTH, Recording.DESIRED_IMAGE_HEIGHT, true, true, true));
    }

    private void showRecordingDetails(Recording recording) {
        setLabelText(title, recording.getSeriesTitle());
        setLabelText(subtitle, recording.getEpisodeTitle());

        setLabelText(episode, recording.getSeasonAndEpisode());
        setLabelText(date, recording.getDateRecorded().toString());
        if (recording.getOriginalAirDate() != null)
            setLabelText(originalAirDate, "Originally aired on " + recording.getOriginalAirDate().toString());
        if (recording.getChannel() != null)
            setLabelText(channel, recording.getChannel().toString());
        if (recording.getDuration() != null)
            setLabelText(duration, recording.getDuration().toString());
        setLabelText(description, recording.getDescription());
        if (recording.getImageURL() != null)
            image.setImage(new Image(recording.getImageURL().toString(),
                    Recording.DESIRED_IMAGE_WIDTH, Recording.DESIRED_IMAGE_HEIGHT, true, true, true));
    }

    private void setLabelText(Label label, String text) {
        if (text != null && text.trim().length() > 0) {
            label.setText(text);
            label.setVisible(true);
            label.setManaged(true);
        } else {
            // Hide this label and remove it from our layout
            label.setVisible(false);
            label.setManaged(false);
        }
    }
}
