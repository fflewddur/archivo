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
import net.straylightlabs.archivo.model.Recording;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// TODO If the recording is from the prior year, append the year to the date
// TODO Recordings from today and yesterday should use those terms instead of a formatted date

public class RecordedOnCellFactory extends TreeTableCell<Recording, LocalDateTime> {
    private static DateTimeFormatter formatter;

    static {
        formatter = DateTimeFormatter.ofPattern("EEE MMM dd");
    }

    @Override
    protected void updateItem(LocalDateTime date, boolean isEmpty) {
        super.updateItem(date, isEmpty);

        Recording recording = null;
        if (getTreeTableRow() != null && getTreeTableRow().getTreeItem() != null) {
            recording = getTreeTableRow().getTreeItem().getValue();
        }
        if (date == null || isEmpty || (recording != null && recording.isSeriesHeading())) {
            setText(null);
            setStyle("");
        } else {
            setText(formatter.format(date));
        }
    }
}
