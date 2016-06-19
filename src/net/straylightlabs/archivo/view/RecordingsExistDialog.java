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
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import net.straylightlabs.archivo.model.Recording;
import net.straylightlabs.archivo.model.UserPrefs;
import org.controlsfx.control.SegmentedButton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Ask user whether to overwrite or rename files that already exist
 */
public class RecordingsExistDialog {
    private final Dialog<ButtonType> dialog;
    private final List<Recording> recordings;
    private final List<Recording> recordingsToDisplay;
    private final Window parent;
    private final UserPrefs userPrefs;

    @SuppressWarnings("unused")
    private final static Logger logger = LoggerFactory.getLogger(RecordingsExistDialog.class);

    public RecordingsExistDialog(Window parent, List<Recording> recordings, List<Recording> toDisplay, UserPrefs userPrefs) {
        dialog = new Dialog<>();
        this.parent = parent;
        this.recordings = recordings;
        this.recordingsToDisplay = toDisplay;
        this.userPrefs = userPrefs;
        initDialog();
    }

    private void initDialog() {
        dialog.initOwner(parent);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.DECORATED);
        dialog.setResizable(true);
        dialog.setTitle("Recordings Already Exist");
        dialog.setHeaderText("Replace or rename recordings?");
        dialog.getDialogPane().setPadding(new Insets(10));

        VBox vbox = new VBox();
        vbox.setSpacing(10);
        vbox.setPrefWidth(800);
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
        scrollPane.setMinHeight(300);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setContent(recordingBox);
        vbox.getChildren().add(scrollPane);

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    }

    private GridPane buildRecordingGrid(Recording recording) {
        GridPane grid = new GridPane();
//        grid.setGridLinesVisible(true);
        grid.setHgap(5);
        grid.setVgap(5);

        Label status = new Label();
        status.setMinWidth(100);
        status.textProperty().bind(recording.fileExistsActionProperty().asString());
        Label title = new Label();
        title.textProperty().bind(recording.fullTitleProperty());
        Label destination = new Label();
        destination.textProperty().bind(recording.destinationProperty().asString());
        ReplaceOrRenameActionBar actionBar = new ReplaceOrRenameActionBar(recording, userPrefs);
        GridPane.setHalignment(actionBar, HPos.RIGHT);
        GridPane.setHgrow(actionBar, Priority.ALWAYS);
//            grid.setFillWidth(title, true);
        grid.add(status, 0, 0, 1, 2);
        grid.add(title, 1, 0);
        grid.add(destination, 1, 1);
        grid.add(actionBar, 2, 0, 1, 2);

        return grid;
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
            SaveFileDialog dialog = new SaveFileDialog(parent, recording, userPrefs);
            if (dialog.showAndWait()) {
                if (destinationExistsOrIsDuplicate(recording)) {
                    recording.setFileExistsAction(Recording.FileExistsAction.REPLACE);
                } else {
                    recording.setFileExistsAction(Recording.FileExistsAction.RENAME);
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
                case RENAME:
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
