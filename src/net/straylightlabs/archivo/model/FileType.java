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

public enum FileType {
    ANDROID_PHONE("Android Files", "*.mp4", "Android"),
    ANDROID_TABLET("Android Tablet Files", "*.mp4", "Android Tablet"),
    APPLE_TV("AppleTV 1 & 2 Files", "*.m4v", "AppleTV"),
    APPLE_TV3("AppleTV 3 Files", "*.m4v", "AppleTV 3"),
    H264_HIGH("Standard H.264 (High Profile) Files ", "*.mp4", "High Profile"),
    H264_NORMAL("Standard H.264 Files", "*.mp4", "Normal"),
    IPAD("iPad Files", "*.mp4", "iPad"),
    IPHONE("iPhone & iPod Touch Files", "*.mp4", "iPhone & iPod touch"),
    TIVO("Encrypted TiVo Files", "*.TiVo", null),
    TS("MPEG-TS Files", "*.ts", null),
    WINDOWS_PHONE("Windows Phone Files", "*.mp4", "Windows Phone 8");

    private final String description;
    private final String extension;
    private final String handbrakePreset;

    FileType(String description, String extension, String preset) {
        this.description = description;
        this.extension = extension;
        this.handbrakePreset = preset;
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

    public String getHandbrakePreset() {
        return handbrakePreset;
    }
}
