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

package net.dropline.archivo;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.dropline.archivo.model.Tivo;
import net.dropline.archivo.view.RecordingListController;
import net.dropline.archivo.view.RootLayoutController;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// TODO Display details of the selected recording below the recording list
// TODO Add control for downloading the selected recording
// TODO Add control for filtering TiVo recommendations from the recording list
// TODO Add control for filtering DRM-protected recordings from the recording list
// TODO Change output statements to use a logging system

public class Archivo extends Application {
    private Stage primaryStage;
    private BorderPane rootLayout;
    private final StringProperty statusText;
    private final ExecutorService executor;
    private final UserPrefs prefs;
    private RootLayoutController rootController;

    public static final String ApplicationName = "Archivo";
    public static final String ApplicationRDN = "net.dropline.archivo";
    public static final String ApplicationVersion = "0.1.0";

    public Archivo() {
        prefs = new UserPrefs();
        statusText = new SimpleStringProperty();
        executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle(ApplicationName);

        initRootLayout();

        String mak = prefs.getMAK();
        if (mak == null) {
            try {
                mak = showSetupDialog(primaryStage);
            } catch (IllegalStateException e) {
                System.err.println("Error: " + e.getLocalizedMessage());
                cleanShutdown();
            }
        }
        List<Tivo> initialTivos = prefs.getKnownDevices(mak);
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
            rootLayout = loader.load();

            rootController = loader.getController();
            rootController.setMainApp(this);

            primaryStage.setScene(new Scene(rootLayout));
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String showSetupDialog(Stage parent) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(Archivo.class.getResource("view/SetupDialog.fxml"));

            Dialog<String> dialog = new Dialog<>();
            dialog.initStyle(StageStyle.UNDECORATED);
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(parent);

            dialog.setDialogPane(loader.load());

            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
            okButton.setDisable(true);

            TextField makNode = (TextField) dialog.getDialogPane().lookup("#mak");
            makNode.textProperty().addListener((observable, oldVal, newVal) -> {
                okButton.setDisable(newVal.trim().isEmpty());
            });

            Platform.runLater(makNode::requestFocus);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == ButtonType.OK) {
                    return makNode.getText();
                }
                return null;
            });

            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                String mak = result.get();
                prefs.setMAK(mak);
                return mak;
            }
        } catch (IOException e) {
            System.err.println("Error loading SetupDialog: " + e.getLocalizedMessage());
        }
        // If we reached this point, we don't have a valid MAK.
        throw new IllegalStateException("We need a valid media access key (MAK) to connect to your TiVo.");
    }

    private void initRecordingList(List<Tivo> initialTivos) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(Archivo.class.getResource("view/RecordingList.fxml"));

            RecordingListController recordingListController = new RecordingListController(this, initialTivos);
            loader.setController(recordingListController);

            Pane recordingList = loader.load();
            rootLayout.setCenter(recordingList);

            recordingListController.startTivoSearch();
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
