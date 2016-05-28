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

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
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
import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.List;

public class RecordingDetailsController implements Initializable {
    private final Archivo mainApp;
    private final Map<URL, Image> imageCache;
    private Recording recording;
    private final ChangeListener<ArchiveStatus> statusChangeListener;

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
    private Label stillRecording;
    @FXML
    private Label expectedDeletion;
    @FXML
    private ImageView poster;
    @FXML
    private Pane posterPane;
    @FXML
    private Button archiveButton;
    @FXML
    private Button cancelButton;
    @FXML
    private Button playButton;

    public RecordingDetailsController(Archivo mainApp) {
        this.mainApp = mainApp;
        imageCache = new HashMap<>();
        statusChangeListener = (observable, oldStatus, newStatus) -> {
            if (oldStatus != newStatus) {
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
            Archivo.logger.info("Archive recording {} to {} (file type = {})...",
                    recording.getFullTitle(), destination, recording.getDestinationType()
            );
            recording.setStatus(ArchiveStatus.QUEUED);
            mainApp.enqueueRecordingForArchiving(recording);
            mainApp.setLastFolder(destination.getParent());
        }
    }

    @FXML
    public void cancel(ActionEvent event) {
        Archivo.logger.info("Cancel archiving of recording {}...", recording.getFullTitle());
        mainApp.cancelArchiving(recording);
        recording.setStatus(ArchiveStatus.EMPTY);
    }

    @FXML
    public void play(ActionEvent event) {
        Archivo.logger.info("Playing recording {}...", recording.getDestination());
        try {
            Desktop.getDesktop().open(recording.getDestination().toFile());
        } catch (IOException e) {
            Archivo.logger.error("Error playing '{}': ", recording.getDestination(), e);
        }
    }

    @FXML
    public void openFolder(ActionEvent event) {
        Archivo.logger.info("Opening folder containing recording {}...", recording.getDestination());
        try {
            Desktop.getDesktop().browse(recording.getDestination().getParent().toUri());
        } catch (IOException e) {
            Archivo.logger.error("Error playing '{}': ", recording.getDestination(), e);
        }
    }

    private Path showSaveDialog(Window parent) {
        FileChooser chooser = new FileChooser();
        setupFileTypes(chooser);
        chooser.setInitialFileName(recording.getDefaultFilename());
        chooser.setInitialDirectory(mainApp.getLastFolder().toFile());

        ObjectProperty<FileChooser.ExtensionFilter> selectedExtensionFilterProperty = chooser.selectedExtensionFilterProperty();
        File destination = chooser.showSaveDialog(parent);
        FileType type = saveFileType(selectedExtensionFilterProperty);
        if (destination != null) {
            recording.setDestination(destination.toPath());
            recording.setDestinationType(type);
            return destination.toPath();
        } else {
            return null;
        }
    }

    private void setupFileTypes(FileChooser chooser) {
        List<FileChooser.ExtensionFilter> fileTypes = new ArrayList<>();
        FileChooser.ExtensionFilter selected = null;
        String previousFileType = mainApp.getUserPrefs().getMostRecentFileType();
        for (FileType type : FileType.values()) {
            FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter(type.getDescription(), type.getExtension());
            fileTypes.add(filter);
            if (type.getDescription().equalsIgnoreCase(previousFileType)) {
                Archivo.logger.info("Setting extension filter: {}", previousFileType);
                selected = filter;
            }
        }
        chooser.getExtensionFilters().addAll(fileTypes);
        if (selected != null) {
            chooser.setSelectedExtensionFilter(selected);
        }
    }

    private FileType saveFileType(ObjectProperty<FileChooser.ExtensionFilter> selectedExtensionFilterProperty) {
        FileChooser.ExtensionFilter filter = selectedExtensionFilterProperty.get();
        FileType fileType = null;
        if (filter != null) {
            String description = filter.getDescription();
            Archivo.logger.info("Selected extension filter: {}", description);
            fileType = FileType.fromDescription(description);
            mainApp.getUserPrefs().setMostRecentType(fileType);
        }
        return fileType;
    }

    private void clearRecording() {
        setLabelText(title, "");
        setLabelText(subtitle, "");
        setLabelText(episode, "");
        setLabelText(date, "");
        setLabelText(originalAirDate, "");
        setLabelText(channel, "");
        setLabelText(duration, "");
        setLabelText(description, "");
        setLabelText(copyProtected, "");
        setLabelText(stillRecording, "");
        setLabelText(expectedDeletion, "");
        setPosterFromURL(null);
        hideNode(archiveButton);
        hideNode(cancelButton);
        hideNode(playButton);
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
        } else if (recording.isInProgress()) {
            setLabelText(stillRecording, "Still recording");
        }
        updateExpectedDeletion(recording);

        updateControls();
    }

    private void updateExpectedDeletion(Recording recording) {
        expectedDeletion.getStyleClass().clear();

        LocalDate today = LocalDate.now();
        Period timeUntilDeletion = today.until(recording.getExpectedDeletion().toLocalDate());
        long daysUntilDeletion = timeUntilDeletion.getDays();
        if (daysUntilDeletion < 1) {
            setLabelText(expectedDeletion, "Will be removed today");
            expectedDeletion.getStyleClass().add("removing-today");
        } else if (daysUntilDeletion < 2) {
            setLabelText(expectedDeletion, "Will be removed tomorrow");
            expectedDeletion.getStyleClass().add("removing-today");
        } else if (daysUntilDeletion < 7) {
            setLabelText(expectedDeletion, String.format("Will be removed in %d days", daysUntilDeletion));
            expectedDeletion.getStyleClass().add("removing-soon");
        } else {
            expectedDeletion.setGraphic(null);
        }
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
        archiveButton.disableProperty().bind(Bindings.or(recording.isArchivableProperty().not(), recording.isCancellableProperty()));

        if (recording == null || recording.isSeriesHeading()) {
            hideNode(archiveButton);
        } else {
            if (recording.isCopyProtected() || recording.isInProgress()) {
                showNode(archiveButton);
            } else if (recording.getStatus().getStatus().isCancelable()) {
                showNode(archiveButton);
                showNode(cancelButton);
                hideNode(playButton);
            } else if (recording.getStatus().getStatus() == ArchiveStatus.TaskStatus.FINISHED) {
                hideNode(archiveButton);
                hideNode(cancelButton);
                showNode(playButton);
            } else {
                showNode(archiveButton);
                hideNode(cancelButton);
                hideNode(playButton);
            }
        }
    }
}
