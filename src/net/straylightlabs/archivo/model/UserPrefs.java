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

package net.straylightlabs.archivo.model;

import javafx.application.Application;
import net.straylightlabs.archivo.Archivo;
import net.straylightlabs.archivo.controller.MAKManager;
import net.straylightlabs.archivo.utilities.OSHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.prefs.Preferences;

public class UserPrefs {
    private Preferences prefs;
    private Preferences sysPrefs;
    private String tooldir;

    private final static Logger logger = LoggerFactory.getLogger(UserPrefs.class);

    private static final String MAK = "mak";
    private static final String MOST_RECENT_DEVICE = "lastTivo";
    private static final String MOST_RECENT_FOLDER = "lastFolder";
    private static final String MOST_RECENT_TYPE = "lastFileType";
    private static final String SKIP_COMMERCIALS = "skipCommercials";
    private static final String HARDWARE_ACCELERATION = "hardwareAcceleration";
    private static final String VIDEO_LIMIT = "maxVideoResolution";
    private static final String AUDIO_LIMIT = "maxAudioChannels";
    private static final String WINDOW_MAXIMIZED = "windowMaximized";
    private static final String WINDOW_HEIGHT = "windowHeight";
    private static final String WINDOW_WIDTH = "windowWidth";
    private static final String COMSKIP_PATH = "comskipPath";
    private static final String FFMPEG_PATH = "ffmpegPath";
    private static final String FFPROBE_PATH = "ffprobePath";
    private static final String HANDBRAKE_PATH = "handbrakePath";
    private static final String SHARE_TELEMETRY = "shareTelemetry";
    private static final String DEBUG_MODE = "debugMode";
    private static final String USER_ID = "userId";

    private static final String DEFAULT_TOOLDIR = ".";

    public UserPrefs() {
        tooldir = DEFAULT_TOOLDIR;

        try {
            prefs = Preferences.userNodeForPackage(Archivo.class);
            sysPrefs = Preferences.systemNodeForPackage(Archivo.class);
        } catch (SecurityException e) {
            logger.error("Error accessing preferences: ", e);
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
        List<String> params = parameters.getUnnamed();
        for (int i = 0; i < params.size(); i++) {
            String param = params.get(i);
            if (param.equalsIgnoreCase("-tooldir")) {
                tooldir = params.get(++i);
                logger.info("Tools in '{}'", tooldir);
            } else {
                logger.error("Unrecognized parameter: {}", param);
                allParsed = false;
            }
        }
        return allParsed;
    }

    public synchronized void loadMAKs(MAKManager manager) {
        String maks = prefs.get(MAK, "");
        manager.load(maks);
    }

    public synchronized void saveMAKs(MAKManager manager) {
        prefs.put(MAK, manager.getAsString());
    }

    public synchronized boolean getSkipCommercials() {
        return prefs.getBoolean(SKIP_COMMERCIALS, true);
    }

    public synchronized void setSkipCommercials(boolean val) {
        prefs.putBoolean(SKIP_COMMERCIALS, val);
    }

    public synchronized boolean getHardwareAcceleration() {
        return prefs.getBoolean(HARDWARE_ACCELERATION, true);
    }

    public synchronized void setHardwareAcceleration(boolean val) {
        prefs.putBoolean(HARDWARE_ACCELERATION, val);
    }

    public synchronized VideoResolution getVideoResolution() {
        int height = prefs.getInt(VIDEO_LIMIT, 1080);
        return VideoResolution.fromHeight(height);
    }

    public synchronized void setVideoResolution(VideoResolution limit) {
        prefs.putInt(VIDEO_LIMIT, limit.getHeight());
    }

    public synchronized AudioChannel getAudioChannels() {
        String channel = prefs.get(AUDIO_LIMIT, "all");
        return AudioChannel.fromChannels(channel);
    }

    @SuppressWarnings("unused")
    public synchronized void setAudioChannels(AudioChannel limit) {
        prefs.put(AUDIO_LIMIT, limit.getChannels());
    }

    public Tivo getLastDevice(final String mak) {
        Tivo lastDevice = null;
        String json = prefs.get(MOST_RECENT_DEVICE, null);
        if (json != null) {
            try {
                logger.info("Last device = {}", json);
                lastDevice = Tivo.fromJSON(json, mak);
            } catch (IllegalArgumentException e) {
                logger.error("Error parsing most recent device: ", e);
            }
        }
        return lastDevice;
    }

    public void setLastDevice(Tivo tivo) {
        if (tivo == null) {
            return;
        }
        prefs.put(MOST_RECENT_DEVICE, tivo.toJSON().toString());
    }

    public Path getLastFolder() {
        Path lastFolder = Paths.get(prefs.get(MOST_RECENT_FOLDER, getPlatformVideoFolder()));
        if (!Files.isDirectory(lastFolder)) {
            logger.error("Last folder was '{}', but this folder no longer exists", lastFolder);
            lastFolder = Paths.get(getPlatformVideoFolder());
        }
        logger.info("Last folder = {}", lastFolder);
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
        return prefs.get(COMSKIP_PATH, sysPrefs.get(COMSKIP_PATH, Paths.get(tooldir, "comskip" + OSHelper.getExeSuffix()).toString()));
    }

    public synchronized String getFFmpegPath() {
        return prefs.get(FFMPEG_PATH, sysPrefs.get(FFMPEG_PATH, Paths.get(tooldir, "ffmpeg" + OSHelper.getExeSuffix()).toString()));
    }

    public synchronized String getFFprobePath() {
        return prefs.get(FFPROBE_PATH, sysPrefs.get(FFPROBE_PATH, Paths.get(tooldir, "ffprobe" + OSHelper.getExeSuffix()).toString()));
    }

    public synchronized String getHandbrakePath() {
        return prefs.get(HANDBRAKE_PATH, sysPrefs.get(HANDBRAKE_PATH, Paths.get(tooldir, "handbrake" + OSHelper.getExeSuffix()).toString()));
    }

    public synchronized boolean getShareTelemetry() {
        return prefs.getBoolean(SHARE_TELEMETRY, sysPrefs.getBoolean(SHARE_TELEMETRY, true));
    }

    public synchronized void setShareTelemetry(boolean val) {
        prefs.putBoolean(SHARE_TELEMETRY, val);
        if (val) {
            Archivo.telemetryController.enable();
        } else {
            Archivo.telemetryController.disable();
        }
    }

    public synchronized boolean getDebugMode() {
        return prefs.getBoolean(DEBUG_MODE, sysPrefs.getBoolean(DEBUG_MODE, false));
    }

    public synchronized void setDebugMode(boolean val) {
        prefs.putBoolean(DEBUG_MODE, val);
    }

    public String getUserId() {
        String userId = prefs.get(USER_ID, null);
        if (userId == null) {
            userId = generateNewUserId();
        }
        return userId;
    }

    private String generateNewUserId() {
        String userId = UUID.randomUUID().toString();
        prefs.put(USER_ID, userId);
        return userId;
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
}
