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
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import net.dropline.archivo.view.RecordingListController;
import net.dropline.archivo.view.RootLayoutController;

import java.io.IOException;

public class MainApp extends Application {
    private Stage primaryStage;
    private BorderPane rootLayout;
    private final StringProperty statusText;

    private RootLayoutController rootController;

    public static final String ApplicationName = "Archivo";
    public static final String ApplicationRDN = "net.dropline.archivo";
    public static final String ApplicationVersion = "0.1.0";

    public static final byte[] testDeviceAddress = {10, 0, 0, 110};
    public static final String testDeviceMAK = "3806772447";

    public MainApp() {
        statusText = new SimpleStringProperty();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle(ApplicationName);

        initRootLayout();

        showRecordingList();

        primaryStage.setOnCloseRequest(e -> {
                    Platform.exit();
                    System.exit(0);
                }
        );
    }

    private void initRootLayout() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("view/RootLayout.fxml"));
            rootLayout = loader.load();

            rootController = loader.getController();
            rootController.setMainApp(this);

            primaryStage.setScene(new Scene(rootLayout));
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showRecordingList() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("view/RecordingList.fxml"));
            Pane recordingList = loader.load();
            rootLayout.setCenter(recordingList);

            RecordingListController controller = loader.getController();
            controller.setMainApp(this);
            controller.startTivoSearch();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

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

    public static void main(String[] args) {
        launch(args);
    }
}
