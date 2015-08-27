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

import javafx.scene.control.TreeTableCell;
import net.straylightlabs.archivo.model.ArchiveStatus;
import net.straylightlabs.archivo.model.Recording;

public class StatusCellFactory extends TreeTableCell<Recording, ArchiveStatus> {
    @Override
    protected void updateItem(ArchiveStatus status, boolean isEmpty) {
        super.updateItem(status, isEmpty);

        if (status != null && status.getStatus() != ArchiveStatus.TaskStatus.NONE) {
            setText(String.format("%s - %f", status.getStatus(), status.getProgress()));
        } else {
            setText(null);
            setStyle("");
        }
    }
}
