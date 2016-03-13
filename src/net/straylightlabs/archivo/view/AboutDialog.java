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

import javafx.scene.control.Button;
import javafx.scene.control.*;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import net.straylightlabs.archivo.Archivo;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Display information about Archivo.
 */
public class AboutDialog {
    private final Dialog dialog;

    private static final String HOMEPAGE = "http://straylightlabs.net/archivo";
    private static final int DIALOG_WIDTH = 400;

    public AboutDialog(Window parent) {
        dialog = new Dialog();
        initDialog(parent);
    }

    private void initDialog(Window parent) {
        dialog.initOwner(parent);
        dialog.initModality(Modality.NONE);
        dialog.initStyle(StageStyle.UTILITY);

        dialog.setTitle("About " + Archivo.APPLICATION_NAME);
        dialog.setGraphic(new ImageView(new Image(Archivo.class.getClassLoader().getResourceAsStream("resources/archivo-64.png"))));
        dialog.setHeaderText(String.format("%s %s", Archivo.APPLICATION_NAME, Archivo.APPLICATION_VERSION));

        VBox pane = new VBox();
        pane.setSpacing(25);

        VBox nestedPane = new VBox();
        nestedPane.setSpacing(3);
        Text text = new Text("\u00a9 2015\u20132016 Straylight Labs LLC");
        Hyperlink link = new Hyperlink(HOMEPAGE);
        link.setOnAction(event -> {
            try {
                Desktop.getDesktop().browse(new URI(HOMEPAGE));
            } catch (URISyntaxException | IOException e) {
                Archivo.logger.error("Error opening web browser: ", e);
            }
        });
        nestedPane.getChildren().addAll(text, link);
        pane.getChildren().add(nestedPane);

//        addWrappedLabel(String.format("Running on Java %s from %s", System.getProperty("java.version"),
//                System.getProperty("java.vendor")), pane);

        addWrappedLabel("Archivo is free software: you can redistribute it and/or modify " +
                "it under the terms of the GNU General Public License as published by " +
                "the Free Software Foundation, either version 3 of the License, or " +
                "(at your option) any later version.", pane);

        addWrappedLabel("Made with \u2665 in Seattle", pane);

        dialog.getDialogPane().setContent(pane);

        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE)).setDefaultButton(true);
    }

    public void show() {
        dialog.show();
    }

    private void addWrappedLabel(String text, Pane pane) {
        Label label = new Label(text);
        label.setPrefWidth(DIALOG_WIDTH);
        label.setWrapText(true);
        pane.getChildren().add(label);
    }
}
