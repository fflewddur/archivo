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

package net.straylightlabs.archivo;

import javafx.application.Application;
import net.straylightlabs.archivo.model.FileType;
import net.straylightlabs.archivo.model.Tivo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class UserPrefs {
    private Preferences prefs;
    private boolean logVerbose;

    public static final String MAK = "mak";
    public static final String DEVICE_LIST = "knownTivos";
    public static final String MOST_RECENT_DEVICE = "lastTivo";
    public static final String MOST_RECENT_FOLDER = "lastFolder";
    public static final String MOST_RECENT_TYPE = "lastFileType";
    public static final String SKIP_COMMERCIALS = "skipCommercials";
    public static final String WINDOW_MAXIMIZED = "windowMaximized";
    public static final String WINDOW_HEIGHT = "windowHeight";
    public static final String WINDOW_WIDTH = "windowWidth";
    public static final String COMSKIP_PATH = "comskipPath";
    public static final String FFMPEG_PATH = "ffmpegPath";
    public static final String HANDBRAKE_PATH = "handbrakePath";

    public UserPrefs() {
        try {
            prefs = Preferences.userNodeForPackage(Archivo.class);
        } catch (SecurityException e) {
            Archivo.logger.error("Error accessing user preferences: ", e);
        }
    }

    /**
     * Parse command-line arguments into user preferences.
     *
     * @param parameters The Application parameters
     * @return false if an unrecognized parameter was passed
     */
    public boolean parseParameters(Application.Parameters parameters) {
        boolean allParsed = true;
        for (String parameter : parameters.getUnnamed()) {
            if (parameter.equalsIgnoreCase("-verbose")) {
                logVerbose = true;
            } else {
                Archivo.logger.error("Unrecognized parameter: {}", parameter);
                allParsed = false;
            }
        }
        return allParsed;
    }

    public boolean isLogVerbose() {
        return logVerbose;
    }

    public synchronized String getMAK() {
        String mak = prefs.get(MAK, null);
        Archivo.logger.info("MAK = {}", mak);
        return mak;
    }

    public synchronized void setMAK(String val) {
        prefs.put(MAK, val);
    }

    public synchronized boolean getSkipCommercials() {
        return prefs.getBoolean(SKIP_COMMERCIALS, true);
    }

    public synchronized void setSkipCommercials(boolean val) {
        prefs.putBoolean(SKIP_COMMERCIALS, val);
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
                    Archivo.logger.info("Known device = {}", json);
                    tivos.add(Tivo.fromJSON(json, mak));
                } catch (IllegalArgumentException e) {
                    Archivo.logger.error("Error building Tivo object from JSON: ", e);
                }
            }
            return tivos;

        } catch (BackingStoreException e) {
            Archivo.logger.error("Error accessing user preferences: ", e);
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
            Archivo.logger.error("Error accessing user preferences: ", e);
        }
    }

    public Tivo getLastDevice(final String mak) {
        Tivo lastDevice = null;
        String json = prefs.get(MOST_RECENT_DEVICE, null);
        if (json != null) {
            try {
                Archivo.logger.info("Last device = {}", json);
                lastDevice = Tivo.fromJSON(json, mak);
            } catch (IllegalArgumentException e) {
                Archivo.logger.error("Error parsing most recent device: ", e);
            }
        }
        return lastDevice;
    }

    public void setLastDevice(Tivo tivo) {
        prefs.put(MOST_RECENT_DEVICE, tivo.toJSON().toString());
    }

    public Path getLastFolder() {
        Path lastFolder = Paths.get(prefs.get(MOST_RECENT_FOLDER, getPlatformVideoFolder()));
        Archivo.logger.info("Last folder = {}", lastFolder);
        return lastFolder;
    }

    public void setLastFolder(Path lastFolder) {
        prefs.put(MOST_RECENT_FOLDER, lastFolder.toString());
    }

    public String getMostRecentFileType() {
        return prefs.get(MOST_RECENT_TYPE, FileType.getDefault().getDescription());
    }

    public void setMostRecentType(FileType type) {
        prefs.put(MOST_RECENT_TYPE, type.getDescription());
    }

    public boolean isWindowMaximized() {
        return prefs.getBoolean(WINDOW_MAXIMIZED, false);
    }

    public void setWindowMaximized(boolean value) {
        prefs.putBoolean(WINDOW_MAXIMIZED, value);
    }

    public int getWindowHeight() {
        return prefs.getInt(WINDOW_HEIGHT, 650);
    }

    public void setWindowHeight(int value) {
        prefs.putInt(WINDOW_HEIGHT, value);
    }

    public int getWindowWidth() {
        return prefs.getInt(WINDOW_WIDTH, 900);
    }

    public void setWindowWidth(int value) {
        prefs.putInt(WINDOW_WIDTH, value);
    }

    public synchronized String getComskipPath() {
        return prefs.get(COMSKIP_PATH, Paths.get(".", "tools", "comskip" + getExeSuffix()).toString());
    }

    public synchronized String getFfmpegPath() {
        return prefs.get(FFMPEG_PATH, Paths.get(".", "tools", "ffmpeg" + getExeSuffix()).toString());
    }

    public synchronized String getHandbrakePath() {
        return prefs.get(HANDBRAKE_PATH, Paths.get(".", "tools", "handbrake" + getExeSuffix()).toString());
    }

    /**
     * Test for the existence of some common folders. If none exist, default to the user's home directory.
     */
    private String getPlatformVideoFolder() {
        String userHomePath = System.getProperty("user.home");
        List<String> possibleFolders = Arrays.asList("Videos", "Movies", "My Videos");
        for (String possibleFolder : possibleFolders) {
            Path videoPath = Paths.get(userHomePath, possibleFolder);
            if (Files.isDirectory(videoPath)) {
                return videoPath.toString();
            }
        }
        return userHomePath;
    }

    private String getExeSuffix() {
        String osName = System.getProperty("os.name");
        if (osName.startsWith("Windows")) {
            return ".exe";
        } else {
            return "";
        }
    }
}
