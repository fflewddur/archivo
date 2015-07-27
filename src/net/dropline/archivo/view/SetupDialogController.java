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
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;

public class SetupDialogController implements Initializable {
    private Node doneButton;

    @FXML
    private DialogPane setupDialog;

    @FXML
    private TextField mak;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ButtonType doneButtonType = new ButtonType("Done", ButtonBar.ButtonData.OK_DONE);
        setupDialog.getButtonTypes().add(doneButtonType);
        doneButton = setupDialog.lookupButton(doneButtonType);
        doneButton.setDisable(mak.getText().trim().length() == 0);

        mak.setOnKeyTyped(event -> {
            doneButton.setDisable(mak.getText().trim().length() == 0);
        });
    }

    public String getMak() {
        return mak.getText().trim();
    }
}
