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

import net.dropline.archivo.model.Tivo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

class UserPrefs {
    private Preferences prefs;

    public static final String MAK = "mak";
    public static final String DEVICE_LIST = "knownTivos";
    public static final String MOST_RECENT_DEVICE = "lastTivo";

    public UserPrefs() {
        try {
            prefs = Preferences.userNodeForPackage(Archivo.class);
        } catch (SecurityException e) {
            System.err.println("Error accessing user preferences: " + e.getLocalizedMessage());
        }
    }

    public String getMAK() {
        return prefs.get(MAK, null);
    }

    public void setMAK(String val) {
        prefs.put(MAK, val);
    }

    /**
     * Retrieve the list of detected TiVos the last time Archivo was run.
     *
     * @return A List of Tivo devices
     */
    public List<Tivo> getKnownDevices(final String mak) {
        Preferences deviceNode = prefs.node(DEVICE_LIST);
        try {
            if (deviceNode == null || deviceNode.keys().length == 0) {
                return Collections.emptyList();
            }

            List<Tivo> tivos = new ArrayList<>();
            for (String key : deviceNode.keys()) {
                String json = deviceNode.get(key, null);
                try {
                    tivos.add(Tivo.fromJSON(json, mak));
                } catch (IllegalArgumentException e) {
                    System.err.println("Error building Tivo object from JSON: " + e.getLocalizedMessage());
                }
            }
            return tivos;

        } catch (BackingStoreException e) {
            System.err.println("Error reading user preferences: " + e.getLocalizedMessage());
        }

        return Collections.emptyList();
    }

    /**
     * Save the list of detected TiVos to use as our initial list next time.
     *
     * @param tivos The List of Tivo devices to save
     */
    public void setKnownDevices(List<Tivo> tivos) {
        try {
            if (prefs.nodeExists(DEVICE_LIST)) {
                // Clear existing device list
                Preferences existingDevices = prefs.node(DEVICE_LIST);
                existingDevices.removeNode();
            }
            Preferences deviceNode = prefs.node(DEVICE_LIST);
            int deviceNum = 1;
            for (Tivo tivo : tivos) {
                String key = String.format("device%02d", deviceNum++);
                deviceNode.put(key, tivo.toJSON().toString());
            }
        } catch (BackingStoreException e) {
            System.err.println("Error reading user preferences: " + e.getLocalizedMessage());
        }
    }

    public Tivo getLastDevice(final String mak) {
        Tivo lastDevice = null;
        String json = prefs.get(MOST_RECENT_DEVICE, null);
        if (json != null) {
            try {
                lastDevice = Tivo.fromJSON(json, mak);
            } catch (IllegalArgumentException e) {
                System.err.println("Error parsing most recent device: " + e.getLocalizedMessage());
            }
        }
        return lastDevice;
    }

    public void setLastDevice(Tivo tivo) {
        prefs.put(MOST_RECENT_DEVICE, tivo.toJSON().toString());
    }

    public void sync() {
        try {
            prefs.flush();
            prefs.sync();
        } catch (BackingStoreException e) {
            System.err.println("Error synchronizing user preferences: " + e.getLocalizedMessage());
        }
    }
}
