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
import javafx.beans.value.ChangeListener;
import net.straylightlabs.archivo.Archivo;
import net.straylightlabs.archivo.controller.MAKManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.prefs.Preferences;

import static net.straylightlabs.archivo.utilities.OSHelper.getArchSuffix;
import static net.straylightlabs.archivo.utilities.OSHelper.getExeSuffix;

public class UserPrefs {
    private Preferences prefs;
    private Preferences sysPrefs;
    private String tooldir;
    private ChangeListener<NetInterface> networkChangedListener;

    private final static Logger logger = LoggerFactory.getLogger(UserPrefs.class);

    private static final String MAK = "mak";
    private static final String MOST_RECENT_DEVICE = "lastTivo";
    private static final String MOST_RECENT_FOLDER = "lastFolder";
    private static final String MOST_RECENT_TYPE = "lastFileType";
    private static final String SKIP_COMMERCIALS = "skipCommercials";
    private static final String HARDWARE_ACCELERATION = "hardwareAcceleration";
    private static final String VIDEO_LIMIT = "maxVideoResolution";
    private static final String AUDIO_LIMIT = "maxAudioChannels";
    private static final String ORGANIZE_SHOWS = "organizeArchivedShows";
    private static final String WINDOW_MAXIMIZED = "windowMaximized";
    private static final String WINDOW_HEIGHT = "windowHeight";
    private static final String WINDOW_WIDTH = "windowWidth";
    private static final String COMSKIP_PATH = "comskipPath";
    private static final String FFMPEG_PATH = "ffmpegPath";
    private static final String FFPROBE_PATH = "ffprobePath";
    private static final String HANDBRAKE_PATH = "handbrakePath";
    private static final String FIND_TIVOS = "findTiVos";
    private static final String TIVO_ADDRESS = "tivoAddress";
    private static final String NETWORK_INTERFACE = "networkInterface";
    private static final String SHARE_TELEMETRY = "shareTelemetry";
    private static final String DEBUG_MODE = "debugMode";
    private static final String USER_ID = "userId";
    private static final String SHOW_DURATION_COL = "showDurationColumn";
    private static final String SHOW_DATE_COL = "showDateColumn";
    private static final String TITLE_COL_WIDTH = "titleColumnWidth";
    private static final String DURATION_COL_WIDTH = "durationColumnWidth";
    private static final String DATE_COL_WIDTH = "dateColumnWidth";
    private static final String STATUS_COL_WIDTH = "statusColumnWidth";

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

    public void addNetworkChangedListener(ChangeListener<NetInterface> listener) {
        networkChangedListener = listener;
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

    public synchronized void setAudioChannels(AudioChannel limit) {
        prefs.put(AUDIO_LIMIT, limit.getChannels());
    }

    public synchronized boolean getOrganizeArchivedShows() {
        return prefs.getBoolean(ORGANIZE_SHOWS, false);
    }

    public synchronized void setOrganizeArchivedShows(boolean value) {
        prefs.putBoolean(ORGANIZE_SHOWS, value);
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

    public boolean getShowDurationColumn() {
        return prefs.getBoolean(SHOW_DURATION_COL, false);
    }

    public void setShowDurationColumn(boolean value) {
        prefs.putBoolean(SHOW_DURATION_COL, value);
    }

    public boolean getShowDateColumn() {
        return prefs.getBoolean(SHOW_DATE_COL, true);
    }

    public void setShowDateColumn(boolean value) {
        prefs.putBoolean(SHOW_DATE_COL, value);
    }

    public int getTitleColumnWidth() {
        return prefs.getInt(TITLE_COL_WIDTH, 400);
    }

    public void setTitleColumnWidth(int value) {
        prefs.putInt(TITLE_COL_WIDTH, value);
    }

    public int getDurationColumnWidth() {
        return prefs.getInt(DURATION_COL_WIDTH, 100);
    }

    public void setDurationColumnWidth(int value) {
        prefs.putInt(DURATION_COL_WIDTH, value);
    }

    public int getDateColumnWidth() {
        return prefs.getInt(DATE_COL_WIDTH, 100);
    }

    public void setDateColumnWidth(int value) {
        prefs.putInt(DATE_COL_WIDTH, value);
    }

    public int getStatusColumnWidth() {
        return prefs.getInt(STATUS_COL_WIDTH, 350);
    }

    public void setStatusColumnWidth(int value) {
        prefs.putInt(STATUS_COL_WIDTH, value);
    }

    public synchronized String getComskipPath() {
        return prefs.get(COMSKIP_PATH, sysPrefs.get(COMSKIP_PATH,
                Paths.get(tooldir, "comskip" + getExeSuffix()).toString()));
    }

    public synchronized String getFFmpegPath() {
        return prefs.get(FFMPEG_PATH, sysPrefs.get(FFMPEG_PATH,
                Paths.get(tooldir, "ffmpeg" + getExeSuffix()).toString()));
    }

    public synchronized String getFFprobePath() {
        return prefs.get(FFPROBE_PATH, sysPrefs.get(FFPROBE_PATH,
                Paths.get(tooldir, "ffprobe" + getExeSuffix()).toString()));
    }

    public synchronized String getHandbrakePath() {
        return prefs.get(HANDBRAKE_PATH, sysPrefs.get(
                HANDBRAKE_PATH, Paths.get(tooldir, "handbrake" + getArchSuffix() + getExeSuffix()).toString()
        ));
    }

    public synchronized NetInterface getNetworkInterface() {
        int hardwareAddressHash = prefs.getInt(NETWORK_INTERFACE, sysPrefs.getInt(
                NETWORK_INTERFACE, NetInterface.DEFAULT_MACHINE_REPRESENTATION.hashCode())
        );

        if (hardwareAddressHash == NetInterface.DEFAULT_MACHINE_REPRESENTATION.hashCode()) {
            logger.info("Using default network interface");
            return new NetInterface();
        } else {
            NetworkInterface ni = getInterfaceByHardwareAddress(hardwareAddressHash);
            logger.info("Using custom network interface: {}", ni);
            if (ni != null) {
                return new NetInterface(ni);
            } else {
                return new NetInterface();
            }
        }
    }

    private NetworkInterface getInterfaceByHardwareAddress(int macHash) {
        NetworkInterface networkInterface = null;
        try {
            for (NetworkInterface anInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                try {
                    byte[] macBytes = anInterface.getHardwareAddress();
                    if (macBytes != null) {
                        String thisMac = new String(anInterface.getHardwareAddress());
                        if (thisMac.hashCode() == macHash) {
                            networkInterface = anInterface;
                        }
                    }
                } catch (SocketException e) {
                    logger.error(
                            "Unable to get hardware address for interface '{}': {}",
                            anInterface.getDisplayName(), e.getLocalizedMessage()
                    );
                }
            }
        } catch (SocketException e) {
            logger.error("Unable to get list of network interfaces: {}", e.getLocalizedMessage());
        }
        return networkInterface;
    }

    public synchronized void setNetworkInterface(NetInterface netInterface) {
        int hardwareAddressHash = prefs.getInt(NETWORK_INTERFACE, sysPrefs.getInt(
                NETWORK_INTERFACE, NetInterface.DEFAULT_MACHINE_REPRESENTATION.hashCode())
        );
        if (netInterface.getMachineHash() != hardwareAddressHash) {
            prefs.putInt(NETWORK_INTERFACE, netInterface.getMachineHash());
            networkChangedListener.changed(null, null, netInterface);
        }
    }

    public synchronized boolean getFindTivos() {
        boolean retval = prefs.getBoolean(FIND_TIVOS, sysPrefs.getBoolean(FIND_TIVOS, true));
        logger.info("Search for TiVos: {}", retval);
        return retval;
    }

    public synchronized void setFindTivos(boolean val) {
        if (getFindTivos() != val) {
            prefs.putBoolean(FIND_TIVOS, val);
            networkChangedListener.changed(null, null, getNetworkInterface());
        }
    }

    public synchronized InetAddress getTivoAddress() {
        String addressString = prefs.get(TIVO_ADDRESS, sysPrefs.get(TIVO_ADDRESS, ""));
        logger.info("Looking for TiVo with address '{}'", addressString);
        InetAddress address = null;
        if (!addressString.isEmpty()) {
            try {
                address = InetAddress.getByName(addressString);
            } catch (UnknownHostException e) {
                logger.error("Address '{}' is invalid: {}", addressString, e.getLocalizedMessage());
            }
        }
        return address;
    }

    public synchronized void setTivoAddress(String address) {
        String oldAddress = prefs.get(TIVO_ADDRESS, sysPrefs.get(TIVO_ADDRESS, ""));
        if (!oldAddress.equalsIgnoreCase(address)) {
            prefs.put(TIVO_ADDRESS, address);
            networkChangedListener.changed(null, null, getNetworkInterface());
        }
    }

    @SuppressWarnings("all")
    public synchronized boolean getShareTelemetry() {
        return Archivo.IS_BETA || prefs.getBoolean(SHARE_TELEMETRY, sysPrefs.getBoolean(SHARE_TELEMETRY, true));
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
