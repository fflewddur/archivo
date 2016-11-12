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
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import net.straylightlabs.archivo.Archivo;
import net.straylightlabs.archivo.model.ArchiveStatus;
import net.straylightlabs.archivo.model.Recording;
import org.controlsfx.glyphfont.FontAwesome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class RecordingDetailsController implements Initializable {
    private final Archivo mainApp;
    private final RecordingSelection recordingSelection;
    private final Map<URL, Image> imageCache;
    private final BooleanProperty showPoster;
    private final StringProperty expectedRemovalText;

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

    public final static Logger logger = LoggerFactory.getLogger(RecordingDetailsController.class);

    public RecordingDetailsController(Archivo mainApp) {
        this.mainApp = mainApp;
        this.recordingSelection = mainApp.getRecordingListController().getRecordingSelection();
        imageCache = new HashMap<>();
        showPoster = new SimpleBooleanProperty(false);
        expectedRemovalText = new SimpleStringProperty();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Only set the preferred height; the width will scale appropriately
        poster.setFitHeight(Recording.DESIRED_IMAGE_HEIGHT);

        setupIcons();
        setupLabelBindings();
        setupControlBindings();
        recordingSelection.posterUrlProperty().addListener((observable, oldValue, newValue) -> {
            setPosterFromURL(newValue);
        });
        recordingSelection.expectedRemovalDateProperty().addListener(((observable, oldValue, newValue) -> {
            updateExpectedDeletion(newValue);
        }));
    }

    private void setupIcons() {
        archiveButton.setGraphic(mainApp.getGlyph(FontAwesome.Glyph.DOWNLOAD));
        cancelButton.setGraphic(mainApp.getGlyph(FontAwesome.Glyph.CLOSE));
        playButton.setGraphic(mainApp.getGlyph(FontAwesome.Glyph.PLAY));
    }

    private void setupLabelBindings() {
        title.textProperty().bind(recordingSelection.showTitleProperty());
        title.visibleProperty().bind(recordingSelection.showTitleProperty().isEmpty().not());
        title.managedProperty().bind(recordingSelection.showTitleProperty().isEmpty().not());

        subtitle.textProperty().bind(recordingSelection.episodeTitleProperty());
        subtitle.visibleProperty().bind(recordingSelection.episodeTitleProperty().isEmpty().not());
        subtitle.managedProperty().bind(recordingSelection.episodeTitleProperty().isEmpty().not());

        description.textProperty().bind(recordingSelection.descriptionProperty());
        description.visibleProperty().bind(recordingSelection.descriptionProperty().isEmpty().not());
        description.managedProperty().bind(recordingSelection.descriptionProperty().isEmpty().not());

        episode.textProperty().bind(recordingSelection.episodeNumberProperty());
        episode.visibleProperty().bind(recordingSelection.episodeNumberProperty().isEmpty().not());
        episode.managedProperty().bind(recordingSelection.episodeNumberProperty().isEmpty().not());

        date.textProperty().bind(recordingSelection.dateRecordedProperty());
        date.visibleProperty().bind(recordingSelection.dateRecordedProperty().isEmpty().not());
        date.managedProperty().bind(recordingSelection.dateRecordedProperty().isEmpty().not());

        originalAirDate.textProperty().bind(recordingSelection.dateFirstAiredProperty());
        originalAirDate.visibleProperty().bind(recordingSelection.dateFirstAiredProperty().isEmpty().not());
        originalAirDate.managedProperty().bind(recordingSelection.dateFirstAiredProperty().isEmpty().not());

        duration.textProperty().bind(recordingSelection.durationProperty());
        duration.visibleProperty().bind(recordingSelection.durationProperty().isEmpty().not());
        duration.managedProperty().bind(recordingSelection.durationProperty().isEmpty().not());

        channel.textProperty().bind(recordingSelection.channelProperty());
        channel.visibleProperty().bind(recordingSelection.channelProperty().isEmpty().not());
        channel.managedProperty().bind(recordingSelection.channelProperty().isEmpty().not());

        expectedDeletion.textProperty().bind(expectedRemovalText);
        expectedDeletion.visibleProperty().bind(recordingSelection.expectedRemovalDateProperty().isNotNull());
        expectedDeletion.managedProperty().bind(recordingSelection.expectedRemovalDateProperty().isNotNull());

        copyProtected.visibleProperty().bind(recordingSelection.isCopyProtectedProperty());
        copyProtected.managedProperty().bind(recordingSelection.isCopyProtectedProperty());

        stillRecording.visibleProperty().bind(recordingSelection.isRecordingProperty());
        stillRecording.visibleProperty().bind(recordingSelection.isRecordingProperty());

        posterPane.visibleProperty().bind(showPoster);
        posterPane.managedProperty().bind(showPoster);
    }

    private void setupControlBindings() {
        archiveButton.visibleProperty().bind(
                Bindings.and(recordingSelection.showControlsProperty(),
                        Bindings.and(cancelButton.visibleProperty().not(), playButton.visibleProperty().not())
                )
        );
        archiveButton.managedProperty().bind(
                Bindings.and(recordingSelection.showControlsProperty(),
                        Bindings.and(cancelButton.visibleProperty().not(), playButton.visibleProperty().not())
                )
        );
        archiveButton.disableProperty().bind(recordingSelection.isArchivableProperty().not());
        cancelButton.visibleProperty().bind(recordingSelection.isCancellableProperty());
        cancelButton.managedProperty().bind(recordingSelection.isCancellableProperty());
        playButton.visibleProperty().bind(recordingSelection.isPlayableProperty());
        playButton.managedProperty().bind(recordingSelection.isPlayableProperty());
    }

    @FXML
    public void archive(ActionEvent event) {
        mainApp.archiveSelection();
    }

    @FXML
    public void cancel(ActionEvent event) {
        for (Recording recording : recordingSelection.getRecordings()) {
            logger.info("Cancel archiving of recording {}...", recording.getFullTitle());
            mainApp.cancelArchiving(recording);
            recording.setStatus(ArchiveStatus.EMPTY);
        }
    }

    @FXML
    public void play(ActionEvent event) {
        for (Recording recording : recordingSelection.getRecordings()) {
            logger.info("Playing recording {}...", recording.getDestination());
            try {
                Desktop.getDesktop().open(recording.getDestination().toFile());
            } catch (IOException e) {
                logger.error("Error playing '{}': ", recording.getDestination(), e);
            }
        }
    }

    @FXML
    public void openFolder(ActionEvent event) {
        for (Recording recording : recordingSelection.getRecordings()) {
            logger.info("Opening folder containing recording {}...", recording.getDestination());
            try {
                Desktop.getDesktop().browse(recording.getDestination().getParent().toUri());
            } catch (IOException e) {
                logger.error("Error playing '{}': ", recording.getDestination(), e);
            }
        }
    }

    @FXML
    public void delete(ActionEvent event) {
        List<Recording> toDelete = recordingSelection.getRecordingsWithChildren().stream().filter(
                recording -> !recording.isSeriesHeading()).collect(Collectors.toList()
        );
        mainApp.deleteFromTivo(toDelete);
    }

    private void updateExpectedDeletion(LocalDateTime expectedRemovalDate) {
        expectedDeletion.getStyleClass().clear();
        expectedRemovalText.setValue("");

        if (expectedRemovalDate == null) {
            return;
        }

        LocalDate today = LocalDate.now();
        Period timeUntilDeletion = today.until(expectedRemovalDate.toLocalDate());
        int daysUntilDeletion = timeUntilDeletion.getDays();
        if (daysUntilDeletion < 1) {
            expectedRemovalText.setValue("Will be removed today");
            expectedDeletion.getStyleClass().add("removing-today");
        } else if (daysUntilDeletion < 2) {
            expectedRemovalText.setValue("Will be removed tomorrow");
            expectedDeletion.getStyleClass().add("removing-today");
        } else if (daysUntilDeletion < 7) {
            expectedRemovalText.setValue(String.format("Will be removed in %d days", daysUntilDeletion));
            expectedDeletion.getStyleClass().add("removing-soon");
        }
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
            showPoster.setValue(true);
        } else {
            showPoster.setValue(false);
        }
    }
}
