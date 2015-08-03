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
import javafx.scene.layout.Pane;
import net.straylightlabs.archivo.model.Recording;

import java.net.URL;
import java.time.Duration;
import java.util.ResourceBundle;

// TODO Only show original air date when it differs from recorded on date
// TODO Duration should let the user know the show is still recording unless it is completed
// FIXME Movies keep showing the poster of the last selected item

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
    private ImageView channelLogo;
    @FXML
    private Label duration;
    @FXML
    private Label description;
    @FXML
    private ImageView poster;
    @FXML
    private Pane posterPane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        clearRecording();
        poster.setFitWidth(Recording.DESIRED_IMAGE_WIDTH);
        poster.setFitHeight(Recording.DESIRED_IMAGE_HEIGHT);
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
        setPoster(null);
        setChannelLogo(null);
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
        int numEpisodes = recording.getNumEpisodes();
        if (numEpisodes == 1) {
            setLabelText(subtitle, String.format("%d episode", numEpisodes));
        } else if (numEpisodes > 1) {
            setLabelText(subtitle, String.format("%d episodes", numEpisodes));
        } else {
            setLabelText(subtitle, "");
        }
        setPoster(recording.getImageURL());
    }

    private void showRecordingDetails(Recording recording) {
        setPoster(recording.getImageURL());

        setLabelText(title, recording.getSeriesTitle());
        setLabelText(subtitle, recording.getEpisodeTitle());
        setLabelText(description, recording.getDescription());

        setLabelText(date, DateUtils.formatRecordedOnDateTime(recording.getDateRecorded()));
        if (recording.getDuration() != null)
            setLabelText(duration, formatDuration(recording.getDuration()));
        if (recording.getChannel() != null) {
            setLabelText(channel, recording.getChannel().toString());
            setChannelLogo(recording.getChannel().getLogoURL());
        }

        setLabelText(episode, recording.getSeasonAndEpisode());
        if (recording.getOriginalAirDate() != null) {
            setLabelText(originalAirDate, "Originally aired on " +
                    recording.getOriginalAirDate().format(DateUtils.DATE_AIRED_FORMATTER));
        }

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

    private void setPoster(URL url) {
        if (url != null) {
            poster.setImage(new Image(url.toString(),
                    Recording.DESIRED_IMAGE_WIDTH, Recording.DESIRED_IMAGE_HEIGHT, true, true, true));
            posterPane.setVisible(true);
            posterPane.setManaged(true);
        } else {
            posterPane.setVisible(false);
            posterPane.setManaged(false);
        }
    }

    private void setChannelLogo(URL url) {
        if (url != null) {
//            channelLogo.setImage(new Image(url.toString(), 60, 60, true, true, true));
//            channelLogo.setVisible(true);
//            channelLogo.setManaged(true);
        } else {
            channelLogo.setVisible(false);
            channelLogo.setManaged(false);
        }
    }

    private static String formatDuration(Duration duration) {
        int hours = (int) duration.toHours();
        int minutes = (int) duration.toMinutes() - (hours * 60);
        int seconds = (int) (duration.getSeconds() % 60);

        // Round so that we're only displaying hours and minutes
        if (seconds >= 30) {
            minutes++;
        }
        if (minutes >= 60) {
            hours++;
            minutes = 0;
        }

        String formatted;
        if (hours > 0) {
            formatted = String.format("%d:%02d hour", hours, minutes);
            if (hours > 1 || minutes > 0)
                formatted += "s";
        } else {
            formatted = String.format("%d minutes", minutes);
        }
        return formatted;
    }
}
