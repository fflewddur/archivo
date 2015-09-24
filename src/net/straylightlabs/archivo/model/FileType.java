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

package net.straylightlabs.archivo.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum FileType {
    ANDROID_PHONE("Android Files", "*.mp4"),
    ANDROID_TABLET("Android Tablet Files", "*.mp4"),
    APPLE_TV("AppleTV 1 & 2 Files", "*.m4v"),
    APPLE_TV3("AppleTV 3 Files", "*.m4v"),
    H264_HIGH("Standard H.264 (High Profile) Files ", "*.mp4"),
    H264_NORMAL("Standard H.264 Files", "*.mp4"),
    IPAD("iPad Files", "*.mp4"),
    IPHONE("iPhone & iPod Touch Files", "*.mp4"),
    TIVO("Encrypted TiVo Files", "*.TiVo", false),
    TS("MPEG-TS Files", "*.ts", false),
    WINDOWS_PHONE("Windows Phone Files", "*.mp4");

    private final String description;
    private final String extension;
    private final boolean needsTranscoding;

    private static final Map<FileType, Map<String, String>> handbrakeArgs;

    static {
        handbrakeArgs = buildHandbrakeArgsMap();
    }

    FileType(String description, String extension) {
        this(description, extension, true);
    }

    FileType(String description, String extension, boolean needsTranscoding) {
        this.description = description;
        this.extension = extension;
        this.needsTranscoding = needsTranscoding;
    }

    public static FileType fromDescription(String description) {
        for (FileType ft : values()) {
            if (ft.description.equalsIgnoreCase(description)) {
                return ft;
            }
        }
        throw new IllegalArgumentException("Unknown description: " + description);
    }

    public static FileType getDefault() {
        return H264_NORMAL;
    }

    public String getDescription() {
        return description;
    }

    public String getExtension() {
        return extension;
    }

    public boolean needsTranscoding() {
        return needsTranscoding;
    }

    public Map<String, String> getHandbrakeArgs() {
        return handbrakeArgs.getOrDefault(this, Collections.<String, String>emptyMap());
    }

    private static Map<FileType, Map<String, String>> buildHandbrakeArgsMap() {
        Map<FileType, Map<String, String>> map = new HashMap<>();

        // AppleTV 3
        String args = String.format("-e x264 -q 20.0 -r 30 --pfr --audio-copy-mask aac,ac3,dtshd,dts,mp3 -a 1,1 " +
                        "-E %s,copy:ac3 -B 160,none -6 dpl2,none -R Auto,Auto -D 1.0,0.0 --audio-fallback  ffac3 -f m4v -4 -5 " +
                        "--loose-anamorphic --modulus 2 -m --x264-preset medium --h264-profile high --h264-level 4.0 -v",
                getPlatformAudioEncoder());
        map.put(APPLE_TV3, parseArgs(args));

        // Normal
        args = String.format("-e x264 -q 20.0 -a 1 -E %s -B 160 -6 dpl2 -R Auto -D 1.0 --audio-copy-mask aac,ac3,dtshd,dts,mp3 " +
                "--audio-fallback ffac3 -f mp4 -5 --loose-anamorphic --modulus 2 -m --x264-preset medium --h264-profile main " +
                "--h264-level 4.0", getPlatformAudioEncoder());
        map.put(H264_NORMAL, parseArgs(args));

        // High Profile
        args = String.format("-e x264  -q 20.0 -a 1,1 -E %s,copy:ac3 -B 160,160 -6 dpl2,none -R Auto,Auto -D 1.0,0.0 " +
                "--audio-copy-mask aac,ac3,dtshd,dts,mp3 --audio-fallback ffac3 -f mp4 -4 -5 --loose-anamorphic " +
                "--modulus 2 -m --x264-preset medium --h264-profile high --h264-level 4.1", getPlatformAudioEncoder());
        map.put(H264_HIGH, parseArgs(args));

        return map;
    }

    private static Map<String, String> parseArgs(String argString) {
        Map<String, String> map = new HashMap<>();

        String[] args = argString.split("\\s+");
        int i;
        for (i = 0; i + 1 < args.length; i++) {
            if (args[i + 1].startsWith("-")) {
                // Single parameter
                map.put(args[i], null);
            } else {
                // Parameter with a value
                map.put(args[i], args[i + 1]);
                i++;
            }
        }
        if (i < args.length) {
            map.put(args[i], null);
        }

        return map;
    }

    public static String getPlatformAudioEncoder() {
        String osName = System.getProperty("os.name");
        if (osName.startsWith("Mac OS X")) {
            return "ca"; // FIXME not sure what this should be
        } else {
            return "fdk_aac";
        }
    }
}
