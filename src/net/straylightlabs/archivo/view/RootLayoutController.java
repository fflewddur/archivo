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
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.GridPane;
import net.straylightlabs.archivo.Archivo;
import net.straylightlabs.archivo.model.ArchiveStatus;
import net.straylightlabs.archivo.model.Recording;

import java.net.URL;
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
        statusChangeListener = (observable, oldValue, newValue) -> {
            if (oldValue.getStatus() != newValue.getStatus()) {
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
        archiveMenuItem.setDisable(false);
        cancelMenuItem.setDisable(true);
        playMenuItem.setDisable(true);
        deleteMenuItem.setDisable(false);

        if (recording == null || recording.isSeriesHeading()) {
            archiveMenuItem.setDisable(true);
            deleteMenuItem.setDisable(true);
        } else {
            if (recording.isCopyProtected()) {
                archiveMenuItem.setDisable(true);
            } else if (recording.getStatus().getStatus().isCancelable()) {
                archiveMenuItem.setDisable(true);
                cancelMenuItem.setDisable(false);
            } else if (recording.getStatus().getStatus() == ArchiveStatus.TaskStatus.FINISHED) {
                playMenuItem.setDisable(false);
            }
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
