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

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Tell the user their MAK was unable to authenticate with TiVo and prompt them to change it.
 */
public class ChangeMAKDialog extends FormattedDialog {
    private String mak;

    public ChangeMAKDialog(Window parent, String currentMak) {
        super(parent);
        this.mak = currentMak;
        initDialog();
    }

    private void initDialog() {
        dialog.setHeaderText("Could not authenticate with TiVo");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        List<Text> text = buildExplanationText();
        TextFlow explanation = new TextFlow(text.toArray(new Text[text.size()]));
        explanation.setPrefWidth(EXPLANATION_WIDTH);
        explanation.setLineSpacing(3);
        grid.add(explanation, 0, 0, 2, 1);

        grid.add(new Label("Media access key"), 0, 1);
        TextField makField = new TextField();
        if (mak != null) {
            makField.setText(mak);
        }
        grid.add(makField, 1, 1);

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK);

        // Only enable the OK button after the user has entered the MAK
        Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(true);
        makField.textProperty().addListener(((observable, oldValue, newValue) -> {
            okButton.setDisable(newValue.trim().isEmpty());
        }));

        dialog.getDialogPane().setContent(grid);

        Platform.runLater(makField::requestFocus);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                return makField.getText().trim();
            }
            return null;
        });
    }

    private List<Text> buildExplanationText() {
        List<Text> textList = new ArrayList<>();

        textList.add(createText("We could not connect to this TiVo using the media access key (MAK) you provided. " +
                "You can find your MAK by viewing "));
        textList.add(createBoldText("My Account"));
        textList.add(createText(" on tivo.com or from the "));
        textList.add(createBoldText("Account & System Information"));
        textList.add(createText(" menu on your TiVo itself."));

        return textList;
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
