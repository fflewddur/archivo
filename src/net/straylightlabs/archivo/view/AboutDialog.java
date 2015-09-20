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

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import net.straylightlabs.archivo.Archivo;

/**
 * Display information about Archivo.
 */
public class AboutDialog {
    private final Dialog dialog;

    public AboutDialog(Window parent) {
        dialog = new Dialog();
        initDialog(parent);
    }

    private void initDialog(Window parent) {
        dialog.initOwner(parent);
        dialog.initStyle(StageStyle.UTILITY);

        dialog.setTitle("About " + Archivo.APPLICATION_NAME);
        dialog.setHeaderText(String.format("%s %s", Archivo.APPLICATION_NAME, Archivo.APPLICATION_VERSION));
        dialog.setContentText(String.format("\u00a9 2015 Straylight Labs LLC.\n\n" +
                                "%s is free software: you can redistribute it and/or modify " +
                                "it under the terms of the GNU General Public License as published by " +
                                "the Free Software Foundation, either version 3 of the License, or " +
                                "(at your option) any later version.\n\n" +
                                "%s is distributed in the hope that it will be useful, " +
                                "but WITHOUT ANY WARRANTY; without even the implied warranty of " +
                                "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the " +
                                "GNU General Public License for more details.\n\n" +
                                "You should have received a copy of the GNU General Public License " +
                                "along with %s. If not, see <http://www.gnu.org/licenses/>.",
                        Archivo.APPLICATION_NAME, Archivo.APPLICATION_NAME, Archivo.APPLICATION_NAME)
        );

        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
    }

    public void show() {
        dialog.show();
    }
}
