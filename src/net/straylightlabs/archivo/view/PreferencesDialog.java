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

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Window;
import net.straylightlabs.archivo.Archivo;
import net.straylightlabs.archivo.model.AudioChannel;
import net.straylightlabs.archivo.model.UserPrefs;
import net.straylightlabs.archivo.model.VideoResolution;
import net.straylightlabs.archivo.utilities.OSHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This dialog is for user-configurable options.
 */
class PreferencesDialog {
    private final Dialog<String> dialog;
    private final UserPrefs userPrefs;

    private final ObservableList<VideoResolution> videoResolutions;
    private final ObservableList<AudioChannel> audioChannels;

    private static final int HEADER_COL = 0;
    private static final int LABEL_COL = 1;
    private static final int CONTROL_COL = 2;

    public PreferencesDialog(Window parent, Archivo mainApp) {
        dialog = new Dialog<>();
        userPrefs = mainApp.getUserPrefs();
        videoResolutions = buildVideoResolutionList();
        audioChannels = buildAudioChannelList();
        initDialog(parent);
    }

    private ObservableList<VideoResolution> buildVideoResolutionList() {
        List<VideoResolution> resolutions = new ArrayList<>();
        resolutions.addAll(Arrays.asList(VideoResolution.values()));
        return FXCollections.observableArrayList(resolutions);
    }

    private ObservableList<AudioChannel> buildAudioChannelList() {
        List<AudioChannel> channels = new ArrayList<>();
        channels.addAll(Arrays.asList(AudioChannel.values()));
        return FXCollections.observableArrayList(channels);
    }

    private void initDialog(Window parent) {
        dialog.initOwner(parent);
        dialog.initModality(Modality.NONE);
        dialog.getDialogPane().setPrefWidth(350);
        dialog.setTitle("Preferences");

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(5, 15, 15, 15));
        grid.setHgap(10);
        grid.setVgap(10);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPrefWidth(10);
        grid.getColumnConstraints().addAll(col1);
        int row = 0;

        Label header = createHeader("Archive Preferences");
        grid.add(header, HEADER_COL, row++, 3, 1);

        CheckBox comskip = new CheckBox("Try to remove commercials");
        comskip.setTooltip(new Tooltip("Try to determine when commercials start and end, and remove them from the final video. May not always be accurate."));
        comskip.setSelected(userPrefs.getSkipCommercials());
        grid.add(comskip, LABEL_COL, row++, 2, 1);

        CheckBox organize = new CheckBox("Organize recordings by show and season");
        organize.setTooltip(new Tooltip("Automatically create a folder for each show and season of the recordings you archive."));
        organize.setSelected(userPrefs.getOrganizeArchivedShows());
        grid.add(organize, LABEL_COL, row++, 2, 1);

        CheckBox qsv = new CheckBox("Use hardware acceleration");
        qsv.setTooltip(new Tooltip("Use Intel Quick Sync Video (if available) to accelerate video conversions. May result in large file sizes."));
        qsv.setSelected(userPrefs.getHardwareAcceleration());
        grid.add(qsv, LABEL_COL, row++, 2, 1);
        if (!OSHelper.isWindows()) {
            // For now, QSV is only supported by the Windows version of Handbrake
            qsv.setSelected(false);
            qsv.setDisable(true);
        }

        Label label = createLabelWithTooltip("Limit video resolution to", "If your selected file type has a larger resolution than this, archived recordings will be scaled down to this size");
        grid.add(label, LABEL_COL, row);
        ChoiceBox<VideoResolution> videoResolution = new ChoiceBox<>(videoResolutions);
        videoResolution.setValue(userPrefs.getVideoResolution());
        grid.add(videoResolution, CONTROL_COL, row);
        row++;

        label = createLabelWithTooltip("Limit audio channels to", "If your selected file type supports multiple audio channels, archived recordings will have their sound limited to these channels");
        grid.add(label, LABEL_COL, row);
        ChoiceBox<AudioChannel> audioChannel = new ChoiceBox<>(audioChannels);
        audioChannel.setValue(userPrefs.getAudioChannels());
        grid.add(audioChannel, CONTROL_COL, row);
        row++;

        header = createHeader("Improve Archivo");
        grid.add(header, HEADER_COL, row++, 3, 1);

        CheckBox telemetry = new CheckBox("Share anonymous data about feature usage");
        telemetry.setTooltip(new Tooltip("This helps the Archivo developers identify features that aren't working as intended"));
        telemetry.setSelected(userPrefs.getShareTelemetry());
        if (Archivo.IS_BETA) {
            telemetry.setDisable(true);
        }
        grid.add(telemetry, LABEL_COL, row++, 2, 1);

        CheckBox debugMode = new CheckBox("Save debugging files\n(May use significant disk space)");
        debugMode.setTooltip(new Tooltip("Only check this box if you're working with the Archivo developers to diagnose a specific problem.\nIt will make Archivo keep intermediate debugging files that are normally deleted, but which can use several gigabytes of disk space."));
        debugMode.setSelected(userPrefs.getDebugMode());
        grid.add(debugMode, LABEL_COL, row, 2, 1);

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                userPrefs.setSkipCommercials(comskip.isSelected());
                userPrefs.setOrganizeArchivedShows(organize.isSelected());
                if (OSHelper.isWindows()) {
                    userPrefs.setHardwareAcceleration(qsv.isSelected());
                }
                userPrefs.setVideoResolution(videoResolution.getValue());
                userPrefs.setAudioChannels(audioChannel.getValue());
                userPrefs.setShareTelemetry(telemetry.isSelected());
                userPrefs.setDebugMode(debugMode.isSelected());
            }
            return null;
        });
    }

    /**
     * Display the dialog.
     */
    public void show() {
        dialog.show();
    }

    private Label createHeader(String text) {
        Label header = new Label(text);
        header.getStyleClass().add("preference-header");
        return header;
    }

    private Label createLabelWithTooltip(String text, String tip) {
        Label label = new Label(text);
        label.setTooltip(new Tooltip(tip));
        return label;
    }
}
