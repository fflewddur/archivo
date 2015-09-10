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

package net.straylightlabs.archivo;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import net.straylightlabs.archivo.controller.ArchiveQueueManager;
import net.straylightlabs.archivo.model.Recording;
import net.straylightlabs.archivo.model.Tivo;
import net.straylightlabs.archivo.view.RecordingDetailsController;
import net.straylightlabs.archivo.view.RecordingListController;
import net.straylightlabs.archivo.view.RootLayoutController;
import net.straylightlabs.archivo.view.SetupDialog;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

// TODO Add timeout for connecting to TiVo
// TODO Display status column showing recording status: already archived, archiving, queued for archiving, still recording, and copy-protected
// TODO Add control for downloading the selected recording
// TODO Add control for filtering TiVo recommendations from the recording list
// TODO Add control for filtering DRM-protected recordings from the recording list
// TODO Remember the user's last sort column and restore it at startup
// TODO Remember the main window's dimensions and restore them at startup
// TODO Implement collection of paths to tools for decoding, stripping commercials, and encoding video
// TODO Implement queue of archival tasks that can be started, maybe paused?, and canceled

public class Archivo extends Application {
    private Stage primaryStage;
    private String mak;
    private final StringProperty statusText;
    private final ExecutorService rpcExecutor;
    private final UserPrefs prefs;
    private RootLayoutController rootController;
    private RecordingListController recordingListController;
    private RecordingDetailsController recordingDetailsController;
    private final ArchiveQueueManager archiveQueueManager;

    public static final Logger logger = Logger.getLogger(Archivo.class.getName());

    public static final String APPLICATION_NAME = "Archivo";
    public static final String APPLICATION_RDN = "net.straylightlabs.archivo";
    public static final String APPLICATION_VERSION = "0.1.0";
    public static final String USER_AGENT = String.format("%s/%s", APPLICATION_NAME, APPLICATION_VERSION);
    public static final int WINDOW_MIN_HEIGHT = 400;
    public static final int WINDOW_MIN_WIDTH = 555;

    public Archivo() {
        super();
        prefs = new UserPrefs();
        statusText = new SimpleStringProperty();
        rpcExecutor = Executors.newSingleThreadExecutor();
        archiveQueueManager = new ArchiveQueueManager(this);
    }

    private void setLogLevel() {
        if (prefs.isLogVerbose()) {
            logger.setLevel(Level.INFO);
        } else {
            logger.setLevel(Level.SEVERE);
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        if (!prefs.parseParameters(getParameters())) {
            cleanShutdown();
        }
        setLogLevel();

        logger.info("Starting up...");

        this.primaryStage = primaryStage;
        this.primaryStage.setTitle(APPLICATION_NAME);
        this.primaryStage.setMinHeight(WINDOW_MIN_HEIGHT);
        this.primaryStage.setMinWidth(WINDOW_MIN_WIDTH);

        initRootLayout();

        mak = prefs.getMAK();
        if (mak == null) {
            try {
                SetupDialog dialog = new SetupDialog(primaryStage);
                mak = dialog.promptUser();
                prefs.setMAK(mak);
            } catch (IllegalStateException e) {
                logger.severe("Error getting MAK from user: " + e.getLocalizedMessage());
                cleanShutdown();
            }
        }
        List<Tivo> initialTivos = prefs.getKnownDevices(mak);
        initRecordingDetails();
        initRecordingList(initialTivos);

        primaryStage.setOnCloseRequest(e -> cleanShutdown());
    }

    public void cleanShutdown() {
        int waitTimeMS = 100;
        int msLimit = 5000;
        if (archiveQueueManager.hasTasks()) {
            try {
                int msWaited = 0;
                archiveQueueManager.cancelAllArchiveTasks();
                while (archiveQueueManager.hasTasks() && msWaited < msLimit) {
                    Thread.sleep(waitTimeMS);
                    msWaited += waitTimeMS;
                }
            } catch (InterruptedException e) {
                logger.severe("Interrupted while waiting for archive tasks to shutdown: " + e.getLocalizedMessage());
            }
        }
        logger.info("Shutting down.");
        Platform.exit();
        System.exit(0);
    }

    private void initRootLayout() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(Archivo.class.getResource("view/RootLayout.fxml"));
            BorderPane rootLayout = loader.load();

            rootController = loader.getController();
            rootController.setMainApp(this);

            Scene scene = new Scene(rootLayout);
            scene.getStylesheets().add("style.css");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error initializing main window: " + e.getLocalizedMessage(), e);
        }
    }

    private void initRecordingList(List<Tivo> initialTivos) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(Archivo.class.getResource("view/RecordingList.fxml"));

            recordingListController = new RecordingListController(this, initialTivos);
            loader.setController(recordingListController);

            Pane recordingList = loader.load();
            rootController.getMainGrid().add(recordingList, 0, 0);

            recordingListController.addRecordingChangedListener(
                    (observable, oldValue, newValue) -> recordingDetailsController.showRecording(newValue)
            );
            recordingListController.startTivoSearch();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error initializing recording list: " + e.getLocalizedMessage(), e);
        }
    }

    private void initRecordingDetails() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(Archivo.class.getResource("view/RecordingDetails.fxml"));

            recordingDetailsController = new RecordingDetailsController(this);
            loader.setController(recordingDetailsController);

            Pane recordingDetails = loader.load();
            rootController.getMainGrid().add(recordingDetails, 0, 1);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error initializing recording details: " + e.getLocalizedMessage(), e);
        }
    }

    public void enqueueRecordingForArchiving(Recording recording) {
        if (!archiveQueueManager.enqueueArchiveTask(recording, getActiveTivo(), getMak())) {
            logger.severe("Error adding recording to queue");
        }
    }

    public void cancelArchiving(Recording recording) {
        archiveQueueManager.cancelArchiveTask(recording);
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public Tivo getActiveTivo() {
        return recordingListController.getSelectedTivo();
    }

    public ExecutorService getRpcExecutor() {
        return rpcExecutor;
    }

    public StringProperty statusTextProperty() {
        return statusText;
    }

    public void setStatusText(String status) {
        logger.info(String.format("Setting status to '%s'", status));
        statusText.set(status);
        rootController.showStatus();
    }

    public void clearStatusText() {
        logger.info("TaskStatus cleared");
        rootController.hideStatus();
    }

    public void showErrorMessage(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Something went wrong...");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public RecordingDetailsController getRecordingDetailsController() {
        return recordingDetailsController;
    }

    public UserPrefs getUserPrefs() {
        return prefs;
    }

    public String getMak() {
        return mak;
    }

    public void setLastDevice(Tivo tivo) {
        prefs.setLastDevice(tivo);
    }

    public Tivo getLastDevice() {
        return prefs.getLastDevice(mak);
    }

    public void setKnownDevices(List<Tivo> tivos) {
        prefs.setKnownDevices(tivos);
    }

    public Path getLastFolder() {
        return prefs.getLastFolder();
    }

    public void setLastFolder(Path lastFolder) {
        prefs.setLastFolder(lastFolder);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
