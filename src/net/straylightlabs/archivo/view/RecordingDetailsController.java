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

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import net.straylightlabs.archivo.Archivo;
import net.straylightlabs.archivo.model.Recording;

import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class RecordingDetailsController implements Initializable {
    private final Archivo mainApp;
    private Map<URL, Image> imageCache;
    private Recording recording;

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
    private Label copyProtected;
    @FXML
    private ImageView poster;
    @FXML
    private Pane posterPane;
    @FXML
    private Button archiveButton;

    public RecordingDetailsController(Archivo mainApp) {
        this.mainApp = mainApp;
        imageCache = new HashMap<>();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        clearRecording();

        // Only set the preferred height; the width will scale appropriately
        poster.setFitHeight(Recording.DESIRED_IMAGE_HEIGHT);
    }

    @FXML
    public void archive(ActionEvent event) {
        Archivo.logger.info(String.format("Archive recording %s...", recording.getTitle()));
        mainApp.enqueueRecordingForArchiving(recording);
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
        setLabelText(copyProtected, "");
        setPosterFromURL(null);
        hideNode(archiveButton);
    }

    public void showRecording(Recording recording) {
        this.recording = recording;

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
        setPosterFromURL(recording.getImageURL());
    }

    private void showRecordingDetails(Recording recording) {
        clearRecording();

        setPosterFromURL(recording.getImageURL());

        setLabelText(title, recording.getSeriesTitle());
        setLabelText(subtitle, recording.getEpisodeTitle());
        setLabelText(description, recording.getDescription());

        setLabelText(date, DateUtils.formatRecordedOnDateTime(recording.getDateRecorded()));
        if (recording.getDuration() != null)
            setLabelText(duration, formatDuration(recording.getDuration(), recording.isInProgress()));
        if (recording.getChannel() != null) {
            setLabelText(channel, String.format(
                    "Channel %s (%s)", recording.getChannel().getNumber(), recording.getChannel().getName()));
        }

        setLabelText(episode, recording.getSeasonAndEpisode());
        if (!recording.isOriginalRecording()) {
            setLabelText(originalAirDate, "Originally aired on " +
                    recording.getOriginalAirDate().format(DateUtils.DATE_AIRED_FORMATTER));
        }
        if (recording.isCopyProtected()) {
            setLabelText(copyProtected, "Copy-protected");
        }

        if (recording.isCopyProtected() || recording.isInProgress()) {
            archiveButton.setDisable(true);
        } else {
            archiveButton.setDisable(false);
        }
        showNode(archiveButton);
    }

    private void setLabelText(Label label, String text) {
        if (text != null && text.trim().length() > 0) {
            label.setText(text);
            showNode(label);
        } else {
            hideNode(label);
        }
    }

    private void hideNode(Node node) {
        node.setVisible(false);
        node.setManaged(false);
    }

    private void showNode(Node node) {
        node.setVisible(true);
        node.setManaged(true);
    }

    private void setPosterFromURL(URL url) {
        if (url != null) {
            Image posterImage = loadImageFromURL(url);
            if (posterImage != null) {
                setPosterFromImage(posterImage);
            }
        } else {
            setPosterFromImage(null);
        }
    }

    private Image loadImageFromURL(URL url) {
        Image cachedImage = imageCache.get(url);
        if (cachedImage == null) {
            ImageDownloadTask downloadTask = new ImageDownloadTask(url);
            // Download and cache the requested image
            downloadTask.setOnSucceeded(event -> {
                Image image = (Image) event.getSource().getValue();
                if (image == null) {
                    throw new NullPointerException();
                }
                setPosterFromImage(image);
                imageCache.put(url, image);
            });
            mainApp.getExecutor().submit(downloadTask);
        }
        return cachedImage;
    }

    private void setPosterFromImage(Image image) {
        if (image != null) {
            poster.setImage(image);
            showNode(posterPane);
        } else {
            hideNode(posterPane);
        }
    }

    private static String formatDuration(Duration duration, boolean inProgress) {
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

        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(String.format("%d:%02d hour", hours, minutes));
            if (hours > 1 || minutes > 0)
                sb.append("s");
        } else {
            sb.append(String.format("%d minute", minutes));
            if (minutes != 1) {
                sb.append("s");
            }
        }
        if (inProgress)
            sb.append(" (still recording)");

        return sb.toString();
    }
}
