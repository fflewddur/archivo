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

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Window;
import net.straylightlabs.archivo.Archivo;
import net.straylightlabs.archivo.UserPrefs;

/**
 * This dialog is for user-configurable options.
 */
public class PreferencesDialog {
    private final Dialog<String> dialog;
    private final Archivo mainApp;
    private final UserPrefs userPrefs;

    public PreferencesDialog(Window parent, Archivo mainApp) {
        dialog = new Dialog<>();
        this.mainApp = mainApp;
        userPrefs = mainApp.getUserPrefs();
        initDialog(parent);
    }

    private void initDialog(Window parent) {
        dialog.initOwner(parent);
        dialog.initModality(Modality.NONE);

        dialog.setTitle("Preferences");

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10, 10, 10, 10));
        grid.setHgap(10);
        grid.setVgap(20);

        grid.add(new Label("Media access key"), 0, 1);
        TextField mak = new TextField();
        mak.setText(userPrefs.getMAK());
        grid.add(mak, 1, 1);

        CheckBox comskip = new CheckBox("Try to remove commercials");
        comskip.setSelected(userPrefs.getSkipCommercials());
        grid.add(comskip, 0, 2, 2, 1);

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Only enable the OK button after the user has entered the MAK
//        Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
//        okButton.setDisable(true);
//        mak.textProperty().addListener(((observable, oldValue, newValue) -> {
//            okButton.setDisable(newValue.trim().isEmpty());
//        }));

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                mainApp.updateMAK(mak.getText());
                userPrefs.setSkipCommercials(comskip.isSelected());
            }
            return null;
        });
    }

    /**
     * Display the dialog.
     */
    public void show() {
        dialog.show();
    }
}
