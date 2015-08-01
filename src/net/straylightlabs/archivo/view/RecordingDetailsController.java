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
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ResourceBundle;

// TODO Only show original air date when it differs from recorded on date
// TODO Generalize recorded on date formatting for use in the RecordingList view
// TODO Duration should let the user know the show is still recording unless it is completed
// FIXME TiVo Suggestions header item continues to show details of last selected item
// FIXME Movies keep showing the image of the last selected item

public class RecordingDetailsController implements Initializable {
    private final static DateTimeFormatter DATE_RECORDED_LONG_DATE_FORMATTER;
    private final static DateTimeFormatter DATE_RECORDED_SHORT_DATE_FORMATTER;
    private final static DateTimeFormatter DATE_RECORDED_TIME_FORMATTER;
    private final static DateTimeFormatter DATE_AIRED_FORMATTER;
    private final static int currentYear;
    private final static int currentDay;

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

    static {
        DATE_RECORDED_LONG_DATE_FORMATTER = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
        DATE_RECORDED_SHORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d");
        DATE_RECORDED_TIME_FORMATTER = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT);
        DATE_AIRED_FORMATTER = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
        currentYear = LocalDateTime.now().getYear();
        currentDay = LocalDateTime.now().getDayOfYear();
    }

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
        setLabelText(description, recording.getDescription());

        setLabelText(date, formatDateTime(recording.getDateRecorded()));
        if (recording.getDuration() != null)
            setLabelText(duration, formatDuration(recording.getDuration()));
        if (recording.getChannel() != null)
            setLabelText(channel, recording.getChannel().toString());

        setLabelText(episode, recording.getSeasonAndEpisode());
        if (recording.getOriginalAirDate() != null) {
            setLabelText(originalAirDate, "Originally aired on " +
                    recording.getOriginalAirDate().format(DATE_AIRED_FORMATTER));
        }

        if (recording.getImageURL() != null) {
            image.setImage(new Image(recording.getImageURL().toString(),
                    Recording.DESIRED_IMAGE_WIDTH, Recording.DESIRED_IMAGE_HEIGHT, true, true, true));
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

    private static String formatDateTime(LocalDateTime dateTime) {
        StringBuilder sb = new StringBuilder();
        sb.append("Recorded ");

        if (dateTime.getDayOfYear() == currentDay) {
            sb.append("today");
        } else if (dateTime.getDayOfYear() == currentDay - 1) {
            sb.append("yesterday");
        } else if (dateTime.getYear() == currentYear) {
            // Don't include the year for recordings from the current year
            sb.append(dateTime.format(DATE_RECORDED_SHORT_DATE_FORMATTER));
        } else {
            sb.append(dateTime.format(DATE_RECORDED_LONG_DATE_FORMATTER));
        }

        sb.append(" at ");
        sb.append(dateTime.format(DATE_RECORDED_TIME_FORMATTER));

        return sb.toString();
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
