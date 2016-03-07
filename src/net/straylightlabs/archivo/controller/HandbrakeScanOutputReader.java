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

package net.straylightlabs.archivo.controller;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HandbrakeScanOutputReader extends ProcessOutputReader {
    private boolean isQuickSyncSupported;

    private static final Pattern QSV = Pattern.compile("Intel Quick Sync Video support: yes");

    public HandbrakeScanOutputReader() {
        super(null);
    }

    @Override
    public void processLine(String line) {
        addLineToOutput(line);
        Matcher matcher = QSV.matcher(line);
        if (matcher.find()) {
            isQuickSyncSupported = true;
        }
    }

    public boolean isQuickSyncSupported() {
        return isQuickSyncSupported;
    }
}
