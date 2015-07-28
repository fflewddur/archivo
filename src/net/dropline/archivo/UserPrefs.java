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

package net.dropline.archivo;

import java.util.prefs.Preferences;

class UserPrefs {
    private Preferences prefs;

    public static final String MAK = "mak";
    public static final String DEVICE_LIST = "knownTivos";
    public static final String MOST_RECENT_DEVICE = "lastTivo";

    public UserPrefs() {
        prefs = Preferences.userNodeForPackage(Archivo.class);
    }

    public String getMAK() {
        return prefs.get(MAK, null);
    }

    public void setMAK(String val) {
        prefs.put(MAK, val);
    }

}
