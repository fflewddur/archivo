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
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.GridPane;
import net.straylightlabs.archivo.Archivo;
import net.straylightlabs.archivo.model.ArchiveStatus;
import net.straylightlabs.archivo.model.Recording;
import net.straylightlabs.archivo.utilities.OSHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Observable;
import java.util.Observer;
import java.util.ResourceBundle;


public class RootLayoutController implements Initializable, Observer {
    private Archivo mainApp;
    private Recording selectedRecording;
    private final ChangeListener<ArchiveStatus> statusChangeListener;
    private final SimpleBooleanProperty trueProperty;

    private static final String ISSUE_URL = "https://github.com/fflewddur/archivo/issues";

    @FXML
    private MenuBar menubar;
    @FXML
    private MenuItem archiveMenuItem;
    @FXML
    private MenuItem cancelMenuItem;
    @FXML
    private MenuItem playMenuItem;
    @FXML
    private MenuItem openFolderMenuItem;
    @FXML
    private MenuItem deleteMenuItem;
    @FXML
    private MenuItem cancelAllMenuItem;
    @FXML
    private MenuItem preferencesMenuItem;
    @FXML
    private MenuItem expandMenuItem;
    @FXML
    private MenuItem collapseMenuItem;
    @FXML
    private GridPane mainGrid;
    @FXML
    private ProgressIndicator statusIndicator;
    @FXML
    private Label statusMessage;

    private final static Logger logger = LoggerFactory.getLogger(RootLayoutController.class);

    public RootLayoutController() {
        statusChangeListener = (observable, oldStatus, newStatus) -> {
            if (oldStatus != newStatus) {
                updateMenuItems(selectedRecording);
            }
        };
        trueProperty = new SimpleBooleanProperty();
        trueProperty.setValue(true);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        menubar.setUseSystemMenuBar(true);
        statusIndicator.setVisible(false);
        setShortcutKeys();
        tweakPlatformSpecificNames();
    }

    private void setShortcutKeys() {
        archiveMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN));
        if (OSHelper.isMacOS()) {
            deleteMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.BACK_SPACE, KeyCombination.SHORTCUT_DOWN));
            preferencesMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.COMMA, KeyCombination.SHORTCUT_DOWN));
        } else {
            deleteMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.DELETE, KeyCombination.SHORTCUT_DOWN));
        }
        cancelMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.ESCAPE));
        expandMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.CLOSE_BRACKET, KeyCombination.SHORTCUT_DOWN));
        collapseMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.OPEN_BRACKET, KeyCombination.SHORTCUT_DOWN));
    }

    private void tweakPlatformSpecificNames() {
        openFolderMenuItem.setText(String.format("Show in %s", OSHelper.getFileBrowserName()));
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
    public void openFolder(ActionEvent event) {
        mainApp.getRecordingDetailsController().openFolder(event);
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
    public void expandShows(ActionEvent event) {
        mainApp.expandShows();
    }

    @FXML
    public void collapseShows(ActionEvent event) {
        mainApp.collapseShows();
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
            logger.error("Could not open log file '{}': ", logPath, e);
        }
    }

    @FXML
    public void reportProblem(ActionEvent event) {
        try {
            Desktop.getDesktop().browse(URI.create(ISSUE_URL));
        } catch (IOException e) {
            logger.error("Could not open issues URL '{}': ", ISSUE_URL, e);
        }
    }

    public void setMainApp(Archivo app) {
        mainApp = app;
        statusMessage.textProperty().bind(mainApp.statusTextProperty());
    }

    public void setMenuBindings(RecordingListController listController) {
        expandMenuItem.disableProperty().bind(Bindings.size(listController.getTivos()).lessThan(1));
        collapseMenuItem.disableProperty().bind(Bindings.size(listController.getTivos()).lessThan(1));
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
            archiveMenuItem.disableProperty().bind(trueProperty);
            cancelMenuItem.disableProperty().bind(trueProperty);
            playMenuItem.disableProperty().bind(trueProperty);
            openFolderMenuItem.disableProperty().bind(trueProperty);
            deleteMenuItem.disableProperty().bind(trueProperty);
        } else {
            archiveMenuItem.disableProperty().bind(Bindings.or(recording.isArchivableProperty().not(), recording.isCancellableProperty()));
            cancelMenuItem.disableProperty().bind(recording.isCancellableProperty().not());
            playMenuItem.disableProperty().bind(recording.isPlayableProperty().not());
            openFolderMenuItem.disableProperty().bind(recording.isPlayableProperty().not());
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
