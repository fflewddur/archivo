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

import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.GridPane;
import net.straylightlabs.archivo.Archivo;
import net.straylightlabs.archivo.model.ArchiveStatus;
import net.straylightlabs.archivo.model.Recording;
import net.straylightlabs.archivo.utilities.OSHelper;

import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Observable;
import java.util.Observer;
import java.util.ResourceBundle;


public class RootLayoutController implements Initializable, Observer {
    private Archivo mainApp;
    private Recording selectedRecording;
    private ChangeListener<ArchiveStatus> statusChangeListener;

    @FXML
    private MenuBar menubar;
    @FXML
    private MenuItem archiveMenuItem;
    @FXML
    private MenuItem cancelMenuItem;
    @FXML
    private MenuItem playMenuItem;
    @FXML
    private MenuItem deleteMenuItem;
    @FXML
    private MenuItem cancelAllMenuItem;
    @FXML
    private GridPane mainGrid;
    @FXML
    private ProgressIndicator statusIndicator;
    @FXML
    private Label statusMessage;


    public RootLayoutController() {
        statusChangeListener = (observable, oldStatus, newStatus) -> {
            if (oldStatus != newStatus) {
                updateMenuItems(selectedRecording);
            }
        };
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        menubar.setUseSystemMenuBar(true);
        statusIndicator.setVisible(false);
    }

    @FXML
    public void archive(ActionEvent event) {
        mainApp.getRecordingDetailsController().archive(event);
    }

    @FXML
    public void cancel(ActionEvent event) {
        mainApp.getRecordingDetailsController().cancel(event);
    }

    @FXML
    public void play(ActionEvent event) {
        mainApp.getRecordingDetailsController().play(event);
    }

    @FXML
    public void delete(ActionEvent event) {
        mainApp.deleteFromTivo(selectedRecording);
    }

    @FXML
    public void cancelAll(ActionEvent event) {
        mainApp.cancelAll();
    }

    @FXML
    public void quit(ActionEvent event) {
        mainApp.cleanShutdown();
    }

    @FXML
    public void showPreferencesDialog(ActionEvent event) {
        PreferencesDialog preferences = new PreferencesDialog(mainApp.getPrimaryStage(), mainApp);
        preferences.show();
    }

    @FXML
    public void showAboutDialog(ActionEvent event) {
        AboutDialog about = new AboutDialog(mainApp.getPrimaryStage());
        about.show();
    }

    @FXML
    public void openLog(ActionEvent event) {
        Path logPath = Paths.get(OSHelper.getDataDirectory().toString(), "log.txt");
        try {
            Desktop.getDesktop().open(logPath.toFile());
        } catch (IOException e) {
            Archivo.logger.error("Could not open log file '{}': ", logPath, e);
        }
    }

    public void setMainApp(Archivo app) {
        mainApp = app;
        statusMessage.textProperty().bind(mainApp.statusTextProperty());
    }

    public void hideStatus() {
        statusIndicator.setVisible(false);
        statusMessage.setVisible(false);
    }

    public void showStatus() {
        statusIndicator.setVisible(true);
        statusMessage.setVisible(true);
    }

    public GridPane getMainGrid() {
        return mainGrid;
    }

    public void disableMenuItems() {
        updateMenuItems(null);
        setCancelAllDisabled(true);
    }

    public void recordingSelected(Recording recording) {
        if (selectedRecording == recording) {
            return;
        }

        if (selectedRecording != null) {
            selectedRecording.statusProperty().removeListener(statusChangeListener);
        }
        selectedRecording = recording;
        if (selectedRecording != null) {
            selectedRecording.statusProperty().addListener(statusChangeListener);
        }
        updateMenuItems(recording);
    }


    private void updateMenuItems(Recording recording) {
        if (recording == null) {
            archiveMenuItem.setDisable(true);
            cancelMenuItem.setDisable(true);
            playMenuItem.setDisable(true);
            deleteMenuItem.setDisable(true);
        } else {
            archiveMenuItem.disableProperty().bind(Bindings.or(recording.isArchivableProperty().not(), recording.isCancellableProperty()));
            cancelMenuItem.disableProperty().bind(recording.isCancellableProperty().not());
            playMenuItem.disableProperty().bind(recording.isPlayableProperty().not());
            deleteMenuItem.disableProperty().bind(recording.isRemovableProperty().not());
        }
    }

    @Override
    public void update(Observable o, Object hasTasksObject) {
        boolean hasTasks = (boolean) hasTasksObject;
        setCancelAllDisabled(!hasTasks);
    }

    private void setCancelAllDisabled(boolean disabled) {
        cancelAllMenuItem.setDisable(disabled);
    }
}
