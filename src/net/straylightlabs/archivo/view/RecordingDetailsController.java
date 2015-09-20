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

import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import net.straylightlabs.archivo.Archivo;
import net.straylightlabs.archivo.model.ArchiveStatus;
import net.straylightlabs.archivo.model.FileType;
import net.straylightlabs.archivo.model.Recording;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.List;

public class RecordingDetailsController implements Initializable {
    private final Archivo mainApp;
    private Map<URL, Image> imageCache;
    private Recording recording;

    private ChangeListener statusChangeListener;

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
    @FXML
    private Button cancelButton;
    @FXML
    private Button openButton;

    public RecordingDetailsController(Archivo mainApp) {
        this.mainApp = mainApp;
        imageCache = new HashMap<>();
        statusChangeListener = (observable, oldValue, newValue) -> {
            ArchiveStatus oldStatus = (ArchiveStatus) oldValue;
            ArchiveStatus newStatus = (ArchiveStatus) newValue;
            if (oldStatus.getStatus() != newStatus.getStatus()) {
                updateControls();
            }
        };
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        clearRecording();

        // Only set the preferred height; the width will scale appropriately
        poster.setFitHeight(Recording.DESIRED_IMAGE_HEIGHT);
    }

    @FXML
    public void archive(ActionEvent event) {
        Path destination = showSaveDialog(mainApp.getPrimaryStage());
        if (destination != null) {
            Archivo.logger.info("Archive recording {} to {}...", recording.getFullTitle(), destination);
            recording.statusProperty().setValue(ArchiveStatus.QUEUED);
            recording.setDestination(destination);
            mainApp.enqueueRecordingForArchiving(recording);
            mainApp.setLastFolder(destination.getParent());
        }
    }

    @FXML
    public void cancel(ActionEvent event) {
        Archivo.logger.info("Cancel archiving of recording {}...", recording.getFullTitle());
        mainApp.cancelArchiving(recording);
        recording.statusProperty().setValue(ArchiveStatus.EMPTY);
    }

    @FXML
    public void open(ActionEvent event) {
        Archivo.logger.info("Opening recording {}...", recording.getDestination());
        try {
            Desktop.getDesktop().open(recording.getDestination().toFile());
        } catch (IOException e) {
            Archivo.logger.error("Error opening '{}': ", recording.getDestination(), e);
        }
    }

    private Path showSaveDialog(Window parent) {
        FileChooser chooser = new FileChooser();
        setupFileTypes(chooser);
        chooser.setInitialFileName(recording.getDefaultFilename());
        chooser.setInitialDirectory(mainApp.getLastFolder().toFile());

        File destination = chooser.showSaveDialog(parent);
        destination = fixFilename(destination, getSelectedFileExtension(chooser));
        saveFileType(chooser);
        if (destination != null) {
            return destination.toPath();
        } else {
            return null;
        }
    }

    private void setupFileTypes(FileChooser chooser) {
        List<FileChooser.ExtensionFilter> fileTypes = new ArrayList<>();
        FileChooser.ExtensionFilter selected = null;
        String previousExtension = mainApp.getUserPrefs().getMostRecentFileType();
        for (FileType type : FileType.values()) {
            FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter(type.getDescription(), type.getExtension());
            fileTypes.add(filter);
            if (type.getExtension().equalsIgnoreCase(previousExtension)) {
                Archivo.logger.info("Setting extension filter: {}", previousExtension);
                selected = filter;
            }
        }
        chooser.getExtensionFilters().addAll(fileTypes);
        if (selected != null) {
            chooser.setSelectedExtensionFilter(selected);
        }
    }

    private void saveFileType(FileChooser chooser) {
        String extension = getSelectedFileExtension(chooser);
        Archivo.logger.info("Selected extension filter: {}", extension);
        mainApp.getUserPrefs().setMostRecentType(FileType.fromExtension(extension));
    }

    private String getSelectedFileExtension(FileChooser chooser) {
        return chooser.getSelectedExtensionFilter().getExtensions().get(0);
    }

    /**
     * The JavaFX FileChoooser has a bug on Mac OS X that messes up file extensions with extra dots.
     */
    private File fixFilename(File destination, String extension) {
        String destString = destination.toString();
        if (!destString.endsWith(extension)) {
            destString = destString.replaceAll("(\\.)+$", "");
            Archivo.logger.info("Destination: {}, extension = {}", destString, extension);
            destination = new File(destString + extension);
        }
        return destination;
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
        hideNode(cancelButton);
        hideNode(openButton);
    }

    public void showRecording(Recording recording) {
        if (this.recording != null) {
            this.recording.statusProperty().removeListener(statusChangeListener);
        }
        this.recording = recording;
        if (this.recording != null) {
            this.recording.statusProperty().addListener(statusChangeListener);
        }

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

        updateControls();
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
            mainApp.getRpcExecutor().submit(downloadTask);
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

    private void updateControls() {
        if (recording == null || recording.isSeriesHeading()) {
            hideNode(archiveButton);
        } else {
            if (recording.isCopyProtected() || recording.isInProgress()) {
                archiveButton.setDisable(true);
                showNode(archiveButton);
            } else if (recording.getStatus().getStatus().isCancelable()) {
                archiveButton.setDisable(true);
                showNode(archiveButton);
                showNode(cancelButton);
                hideNode(openButton);
            } else if (recording.getStatus().getStatus() == ArchiveStatus.TaskStatus.FINISHED) {
                hideNode(archiveButton);
                hideNode(cancelButton);
                showNode(openButton);
            } else {
                archiveButton.setDisable(false);
                showNode(archiveButton);
                hideNode(cancelButton);
                hideNode(openButton);
            }
        }
    }
}
