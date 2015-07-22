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
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import net.dropline.archivo.view.RecordingListController;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.IOException;

public class MainApp extends Application {
    private Stage primaryStage;
    private BorderPane rootLayout;

    public static final String ApplicationName = "Archivo";
    public static final String ApplicationRDN = "net.dropline.archivo";
    public static final String ApplicationVersion = "0.1.0";

    public static final byte[] testDeviceAddress = {10, 0, 0, 110};
    public static final String testDeviceMAK = "3806772447";

    public MainApp() {
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle(ApplicationName);

        initRootLayout();

        showRecordingList();
    }

    private void initRootLayout() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("view/RootLayout.fxml"));
            rootLayout = loader.load();

            MenuBar mb = (MenuBar) rootLayout.lookup("#menubar");
            mb.setUseSystemMenuBar(true);

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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
//        testDNS();
        launch(args);
    }

    public static void testDNS() {
        JmDNS jmdns = null;
        try {
            jmdns = JmDNS.create();
            while (true) {
                ServiceInfo[] infos = jmdns.list("_http._tcp.local.");
                System.out.println("List _http._tcp.local.");
                for (int i = 0; i < infos.length; i++) {
                    System.out.println(infos[i]);
                }
                System.out.println();

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (jmdns != null) try {
                jmdns.close();
            } catch (IOException exception) {
                //
            }
        }
    }
}
