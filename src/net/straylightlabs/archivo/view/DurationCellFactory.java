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

import javafx.scene.control.TreeTableCell;
import net.straylightlabs.archivo.model.Recording;

import java.time.Duration;

public class DurationCellFactory extends TreeTableCell<Recording, Duration> {
    @Override
    protected void updateItem(Duration duration, boolean isEmpty) {
        super.updateItem(duration, isEmpty);

        Recording recording = null;
        if (getTreeTableRow() != null && getTreeTableRow().getTreeItem() != null) {
            recording = getTreeTableRow().getTreeItem().getValue();
        }
        if (duration == null || recording == null || isEmpty || recording.isSeriesHeading()) {
            setText(null);
            setStyle("");
        } else {
            setText(DateUtils.formatDuration(recording.getDuration(), recording.isInProgress()));
        }
    }
}
