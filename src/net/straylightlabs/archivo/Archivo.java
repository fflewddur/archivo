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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import net.straylightlabs.archivo.model.Tivo;
import net.straylightlabs.archivo.view.RecordingDetailsController;
import net.straylightlabs.archivo.view.RecordingListController;
import net.straylightlabs.archivo.view.RootLayoutController;
import net.straylightlabs.archivo.view.SetupDialog;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// TODO Display status column showing recording status: already archived, archiving, queued for archiving, still recording, and copy-protected
// TODO Add control for downloading the selected recording
// TODO Add control for filtering TiVo recommendations from the recording list
// TODO Add control for filtering DRM-protected recordings from the recording list
// TODO Change output statements to use a logging system
// TODO Remember the user's last sort column and restore it at startup
// TODO Remember the main window's dimensions and restore them at startup
// TODO Implement collection of paths to tools for decoding, stripping commercials, and encoding video
// TODO Implement queue of archival tasks that can be started, maybe paused?, and canceled

public class Archivo extends Application {
    private Stage primaryStage;
    private final StringProperty statusText;
    private final ExecutorService executor;
    private final UserPrefs prefs;
    private RootLayoutController rootController;
    private RecordingDetailsController recordingDetailsController;

    public static final String APPLICATION_NAME = "Archivo";
    public static final String APPLICATION_RDN = "net.straylightlabs.archivo";
    public static final String APPLICATION_VERSION = "0.1.0";
    public static final int WINDOW_MIN_HEIGHT = 400;
    public static final int WINDOW_MIN_WIDTH = 555;

    public Archivo() {
        prefs = new UserPrefs();
        statusText = new SimpleStringProperty();
        executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle(APPLICATION_NAME);
        this.primaryStage.setMinHeight(WINDOW_MIN_HEIGHT);
        this.primaryStage.setMinWidth(WINDOW_MIN_WIDTH);

        initRootLayout();

        String mak = prefs.getMAK();
        if (mak == null) {
            try {
                SetupDialog dialog = new SetupDialog(primaryStage);
                mak = dialog.promptUser();
                prefs.setMAK(mak);
            } catch (IllegalStateException e) {
                System.err.println("Error: " + e.getLocalizedMessage());
                cleanShutdown();
            }
        }
        List<Tivo> initialTivos = prefs.getKnownDevices(mak);
        initRecordingDetails();
        initRecordingList(initialTivos);

        primaryStage.setOnCloseRequest(e -> cleanShutdown());
    }

    private void cleanShutdown() {
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

            primaryStage.setScene(new Scene(rootLayout));
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initRecordingList(List<Tivo> initialTivos) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(Archivo.class.getResource("view/RecordingList.fxml"));

            RecordingListController recordingListController = new RecordingListController(this, initialTivos);
            loader.setController(recordingListController);

            Pane recordingList = loader.load();
            rootController.getMainGrid().add(recordingList, 0, 0);

            recordingListController.addRecordingChangedListener(
                    (observable, oldValue, newValue) -> recordingDetailsController.showRecording(newValue)
            );
            recordingListController.startTivoSearch();
        } catch (IOException e) {
            e.printStackTrace();
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
            e.printStackTrace();
        }
    }

    public ExecutorService getExecutor() {
        return executor;
    }

//    public Stage getPrimaryStage() {
//        return primaryStage;
//    }

    public StringProperty statusTextProperty() {
        return statusText;
    }

    public void setStatusText(String status) {
        statusText.set(status);
        rootController.showStatus();
    }

    public void clearStatusText() {
        rootController.hideStatus();
    }

    public String getMak() {
        return prefs.getMAK();
    }

    public void setLastDevice(Tivo tivo) {
        prefs.setLastDevice(tivo);
    }

    public Tivo getLastDevice() {
        return prefs.getLastDevice(prefs.getMAK());
    }

    public void setKnownDevices(List<Tivo> tivos) {
        prefs.setKnownDevices(tivos);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
