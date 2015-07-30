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

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.text.*;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * This dialog is for capturing necessary first-run setup information.
 * Currently this only includes the media access key (MAK) needed to
 * interact with the user's TiVo devices.
 */
public class SetupDialog {
    private final Dialog<String> dialog;
    private static final double fontSize;
    private static final String fontFamily;

    private static final int EXPLANATION_WIDTH = 400;

    static {
        fontFamily = Font.getDefault().getFamily();
        fontSize = Font.getDefault().getSize();
    }

    public SetupDialog(Window parent) {
        dialog = new Dialog<>();
        initDialog(parent);
    }

    private void initDialog(Window parent) {
        dialog.initOwner(parent);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);

        dialog.setTitle("Archivo Setup");
        dialog.setHeaderText("Welcome to Archivo");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        List<Text> text = buildExplanationText();
        TextFlow explanation = new TextFlow(text.toArray(new Text[text.size()]));
        explanation.setPrefWidth(EXPLANATION_WIDTH);
        explanation.setLineSpacing(3);
        grid.add(explanation, 0, 0, 2, 1);

        grid.add(new Label("Media access key"), 0, 1);
        TextField mak = new TextField();
        grid.add(mak, 1, 1);

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Only enable the OK button after the user has entered the MAK
        Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(true);
        mak.textProperty().addListener(((observable, oldValue, newValue) -> {
            okButton.setDisable(newValue.trim().isEmpty());
        }));

        dialog.getDialogPane().setContent(grid);

        Platform.runLater(mak::requestFocus);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                return mak.getText().trim();
            }
            return null;
        });
    }

    private List<Text> buildExplanationText() {
        List<Text> textList = new ArrayList<>();

        textList.add(createText("Before transferring recordings from TiVo to your computer, you'll need to provide the "));
        textList.add(createItalicsText("media access key (MAK)"));
        textList.add(createText(" associated with your TiVo account. You can find your MAK by viewing "));
        textList.add(createBoldText("My Account"));
        textList.add(createText(" on tivo.com or from the "));
        textList.add(createBoldText("Account & System Information"));
        textList.add(createText(" menu on your TiVo itself."));
        textList.add(createText("\n\nYou can change your MAK at any time from Archivo's Preferences."));

        return textList;
    }

    private Text createText(String s) {
        Text text = new Text(s);
        text.setFont(Font.font(fontFamily, fontSize));
        return text;
    }

    private Text createItalicsText(String s) {
        Text text = new Text(s);
        text.setFont(Font.font(fontFamily, FontPosture.ITALIC, fontSize));
        return text;
    }

    private Text createBoldText(String s) {
        Text text = new Text(s);
        text.setFont(Font.font(fontFamily, FontWeight.BOLD, fontSize));
        return text;
    }

    /**
     * Display the modal dialog.
     *
     * @return The MAK needed to interact with the user's TiVo devices
     */
    public String promptUser() {
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            return result.get();
        }

        // If we reached this point, we don't have a valid MAK and can't continue.
        throw new IllegalStateException("We need a valid media access key (MAK) to connect to your TiVo.");
    }
}
