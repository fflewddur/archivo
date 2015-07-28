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

package net.dropline.archivo.view;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ProgressIndicator;
import net.dropline.archivo.Archivo;

import java.net.URL;
import java.util.ResourceBundle;


public class RootLayoutController implements Initializable {
    @FXML
    private MenuBar menubar;
    @FXML
    private ProgressIndicator statusIndicator;
    @FXML
    private Label statusMessage;

    private Archivo mainApp;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        menubar.setUseSystemMenuBar(true);
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
}
