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

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import net.straylightlabs.archivo.Archivo;
import net.straylightlabs.archivo.model.Recording;
import net.straylightlabs.archivo.model.UserPrefs;
import org.controlsfx.control.SegmentedButton;
import org.controlsfx.glyphfont.FontAwesome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Ask user whether to overwrite or rename files that already exist
 */
public class RecordingsExistDialog {
    private final Dialog<ButtonType> dialog;
    private final List<Recording> recordings;
    private final List<Recording> recordingsToDisplay;
    private final UserPrefs userPrefs;
    private final Archivo mainApp;

    private static final int STATUS_MIN_WIDTH = 30;
    private static final int TITLE_MIN_WIDTH = 200;
    private static final int ACTION_BAR_MIN_WIDTH = 200;

    @SuppressWarnings("unused")
    private final static Logger logger = LoggerFactory.getLogger(RecordingsExistDialog.class);

    public RecordingsExistDialog(Archivo mainApp, List<Recording> recordings, List<Recording> toDisplay, UserPrefs userPrefs) {
        dialog = new Dialog<>();
        this.mainApp = mainApp;
        this.recordings = recordings;
        this.recordingsToDisplay = toDisplay;
        this.userPrefs = userPrefs;
        initDialog();
    }

    private void initDialog() {
        dialog.initOwner(mainApp.getPrimaryStage());
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.DECORATED);
        dialog.setResizable(true);
        dialog.setTitle("Recordings Already Exist");
        dialog.setHeaderText("Replace or rename recordings?");
        dialog.getDialogPane().setPadding(new Insets(10));

        VBox vbox = new VBox();
        vbox.setSpacing(10);
        dialog.getDialogPane().setContent(vbox);

        Label label = new Label("Archiving the following recordings will replace files on your computer unless you rename them:");
        vbox.getChildren().add(label);

        VBox recordingBox = new VBox();
        recordingBox.setSpacing(20);
        for (Recording recording : recordingsToDisplay) {
            recording.setFileExistsAction(Recording.FileExistsAction.REPLACE);
            recordingBox.getChildren().add(buildRecordingGrid(recording));
        }

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.getStyleClass().add("recording-exists-list");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setContent(recordingBox);
        scrollPane.setPadding(new Insets(10));
        vbox.getChildren().add(scrollPane);

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    }

    private GridPane buildRecordingGrid(Recording recording) {
        GridPane grid = new GridPane();
        grid.setHgap(5);
        grid.setVgap(5);

        Label status = new Label();
        status.setAlignment(Pos.CENTER);
        status.setMinWidth(STATUS_MIN_WIDTH);
        updateStatusIcon(recording, status);
        recording.fileExistsActionProperty().addListener(observable -> updateStatusIcon(recording, status));

        Label title = new Label();
        title.setMinWidth(TITLE_MIN_WIDTH);
        title.textProperty().bind(recording.fullTitleProperty());

        Label destination = new Label();
        destination.getStyleClass().add("destination");
        destination.textProperty().bind(recording.destinationProperty().asString());
        LocalDate dateArchived = recording.getDateArchived();
        if (dateArchived != null) {
            String dateArchivedText = String.format("Archived %s", DateUtils.formatArchivedOnDate(dateArchived));
            Tooltip tooltip = new Tooltip(dateArchivedText);
            title.setTooltip(tooltip);
            destination.setTooltip(tooltip);
        }

        ReplaceOrRenameActionBar actionBar = new ReplaceOrRenameActionBar(recording, userPrefs);
        actionBar.setMinWidth(ACTION_BAR_MIN_WIDTH);
        GridPane.setHalignment(actionBar, HPos.RIGHT);
        GridPane.setHgrow(actionBar, Priority.ALWAYS);
        GridPane.setMargin(actionBar, new Insets(0, 0, 0, 10));

        grid.add(status, 0, 0, 1, 2);
        grid.add(title, 1, 0);
        grid.add(destination, 1, 1);
        grid.add(actionBar, 2, 0, 1, 2);

        return grid;
    }

    private void updateStatusIcon(Recording recording, Label status) {
        switch (recording.getFileExistsAction()) {
            case OK:
                status.setGraphic(mainApp.getGlyph(FontAwesome.Glyph.CHECK));
                break;
            case REPLACE:
                status.setGraphic(mainApp.getGlyph(FontAwesome.Glyph.EXCLAMATION_CIRCLE));
                break;
            case CANCEL:
                status.setGraphic(mainApp.getGlyph(FontAwesome.Glyph.CLOSE));
                break;
        }
    }

    public boolean showAndWait() {
        Optional<ButtonType> response = dialog.showAndWait();
        response.filter(r -> r == ButtonType.CANCEL).ifPresent(r -> clearRecordingList());
        response.filter(r -> r == ButtonType.OK).ifPresent(r -> clearCanceledRecordings());
        return (response.isPresent() && response.get() == ButtonType.OK);
    }

    private void clearRecordingList() {
        recordings.clear();
    }

    private void clearCanceledRecordings() {
        List<Recording> toCancel = recordings.stream().filter(
                r -> r.getFileExistsAction() == Recording.FileExistsAction.CANCEL
        ).collect(Collectors.toList());
        toCancel.forEach(recordings::remove);
    }

    public static List<Recording> getRecordingsWithDuplicateDestination(List<Recording> recordings) {
        Set<Path> seen = new HashSet<>();
        Set<Path> duplicatePaths = new HashSet<>();
        for (Recording recording : recordings) {
            Path destination = recording.getDestination();
            if (seen.contains(destination)) {
                duplicatePaths.add(destination);
            } else {
                seen.add(destination);
            }
        }

        return recordings.stream().filter(
                recording -> duplicatePaths.contains(recording.getDestination())).collect(Collectors.toList()
        );
    }

    private class ReplaceOrRenameActionBar extends SegmentedButton {
        private final ToggleButton replace;
        private final ToggleButton rename;
        private final ToggleButton cancel;
        private final ToggleGroup toggleGroup;
        private final UserPrefs userPrefs;
        private Recording recording;

        public ReplaceOrRenameActionBar(Recording recording, UserPrefs userPrefs) {
            this.recording = recording;
            this.userPrefs = userPrefs;
            replace = new ToggleButton("Replace");
            replace.setSelected(true);
            rename = new ToggleButton("Rename");
            cancel = new ToggleButton("Don't Archive");
            toggleGroup = new ToggleGroup();
            toggleGroup.getToggles().setAll(replace, rename, cancel);
            this.getButtons().addAll(replace, rename, cancel);
            replace.setOnAction(event -> replace());
            rename.setOnAction(event -> renameDestination());
            cancel.setOnAction(event -> cancelArchive());
        }

        private void replace() {
            recording.setFileExistsAction(Recording.FileExistsAction.REPLACE);
            replace.setSelected(true);
        }

        private void renameDestination() {
            SaveFileDialog dialog = new SaveFileDialog(mainApp.getPrimaryStage(), recording, userPrefs);
            if (dialog.showAndWait()) {
                if (destinationExistsOrIsDuplicate(recording)) {
                    recording.setFileExistsAction(Recording.FileExistsAction.REPLACE);
                } else {
                    recording.setFileExistsAction(Recording.FileExistsAction.OK);
                }
            }
            setActiveButton();
        }

        /**
         * Return true if @recording's destination already exists or is a duplicate of another queued recording
         */
        private boolean destinationExistsOrIsDuplicate(Recording recording) {
            if (Files.exists(recording.getDestination())) {
                return true;
            }

            for (Recording earlierRecording : recordings) {
                if (earlierRecording == recording) {
                    break;
                } else {
                    if (recording.getDestination().equals(earlierRecording.getDestination())) {
                        return true;
                    }
                }
            }

            return false;
        }

        private void cancelArchive() {
            recording.setFileExistsAction(Recording.FileExistsAction.CANCEL);
            cancel.setSelected(true);
        }

        private void setActiveButton() {
            switch (recording.getFileExistsAction()) {
                case OK:
                    rename.setSelected(true);
                    break;
                case REPLACE:
                    replace.setSelected(true);
                    break;
                case CANCEL:
                    cancel.setSelected(true);
                    break;
            }
        }
    }
}
