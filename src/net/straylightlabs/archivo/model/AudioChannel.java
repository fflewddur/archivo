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

public enum AudioChannel {
    SURROUND("Surround sound", "all"),
    STEREO("Stereo", "2"),
    MONO("Mono", "1");

    private final String label;
    private final String channels;

    AudioChannel(String label, String channels) {
        this.label = label;
        this.channels = channels;
    }

    public static AudioChannel fromChannels(String channels) {
        switch (channels) {
            case "2":
                return AudioChannel.STEREO;
            case "1":
                return AudioChannel.MONO;
            default:
                return AudioChannel.SURROUND;
        }
    }

    public String getChannels() {
        return channels;
    }

    @Override
    public String toString() {
        return label;
    }
}
