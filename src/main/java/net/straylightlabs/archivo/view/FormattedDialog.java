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

import javafx.scene.control.Dialog;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * Base class for creating dialogs with formatted text.
 */
public abstract class FormattedDialog {
    protected final Dialog<String> dialog;
    protected static final double fontSize;
    protected static final String fontFamily;

    protected static final int EXPLANATION_WIDTH = 400;

    static {
        fontFamily = Font.getDefault().getFamily();
        fontSize = Font.getDefault().getSize();
    }

    public FormattedDialog(Window parent) {
        dialog = new Dialog<>();
        initDialog(parent);
    }

    private void initDialog(Window parent) {
        dialog.initOwner(parent);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initStyle(StageStyle.UTILITY);
    }

    protected Text createText(String s) {
        Text text = new Text(s);
        text.setFont(Font.font(fontFamily, fontSize));
        return text;
    }

    protected Text createItalicsText(String s) {
        Text text = new Text(s);
        text.setFont(Font.font(fontFamily, FontPosture.ITALIC, fontSize));
        return text;
    }

    protected Text createBoldText(String s) {
        Text text = new Text(s);
        text.setFont(Font.font(fontFamily, FontWeight.BOLD, fontSize));
        return text;
    }
}
