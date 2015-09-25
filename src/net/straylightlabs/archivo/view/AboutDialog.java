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

import javafx.scene.control.Button;
import javafx.scene.control.*;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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

    private static final String HOMEPAGE = "https://github.com/fflewddur/archivo";

    public AboutDialog(Window parent) {
        dialog = new Dialog();
        initDialog(parent);
    }

    private void initDialog(Window parent) {
        dialog.initOwner(parent);
        dialog.initModality(Modality.NONE);
        dialog.initStyle(StageStyle.UTILITY);

        dialog.setTitle("About " + Archivo.APPLICATION_NAME);
        dialog.setGraphic(new ImageView(new Image("archivo-64.png")));
        dialog.setHeaderText(String.format("%s %s", Archivo.APPLICATION_NAME, Archivo.APPLICATION_VERSION));

        VBox pane = new VBox();
        pane.setSpacing(10);

        Text text = new Text("\u00a9 2015 Straylight Labs LLC");
        Hyperlink link = new Hyperlink(HOMEPAGE);
        link.setOnAction(event -> {
            try {
                Desktop.getDesktop().browse(new URI(HOMEPAGE));
            } catch (URISyntaxException | IOException e) {

            }
        });
        pane.getChildren().addAll(text, link);

        Label label = new Label("Archivo is free software: you can redistribute it and/or modify " +
                "it under the terms of the GNU General Public License as published by " +
                "the Free Software Foundation, either version 3 of the License, or " +
                "(at your option) any later version.");
        label.setPrefWidth(400);
        label.setWrapText(true);
        pane.getChildren().add(label);


//        TextFlow textFlow = new TextFlow(text, link);

        dialog.getDialogPane().setContent(pane);
//        dialog.setContentText(String.format("\u00a9 2015 Straylight Labs LLC.\n" +
//                                "https://github.com/fflewddur/archivo\n\n" +
//                                "%s is free software: you can redistribute it and/or modify " +
//                                "it under the terms of the GNU General Public License as published by " +
//                                "the Free Software Foundation, either version 3 of the License, or " +
//                                "(at your option) any later version.\n\n" +
//                                "%s is distributed in the hope that it will be useful, " +
//                                "but WITHOUT ANY WARRANTY; without even the implied warranty of " +
//                                "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the " +
//                                "GNU General Public License for more details.\n\n" +
//                                "You should have received a copy of the GNU General Public License " +
//                                "along with %s. If not, see <http://www.gnu.org/licenses/>.\n\n" +
//                                "Logo designed by Freepik",
//                        Archivo.APPLICATION_NAME, Archivo.APPLICATION_NAME, Archivo.APPLICATION_NAME)
//        );

        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE)).setDefaultButton(true);
    }

    public void show() {
        dialog.show();
    }
}
