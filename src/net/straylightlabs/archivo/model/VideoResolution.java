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

public enum VideoResolution {
    HD_1080("1080p", 1080, 1920),
    HD_720("720p", 720, 1280),
    SD_480("480p", 480, 720);

    private final String label;
    private final int height;
    private final int width;

    VideoResolution(String label, int height, int width) {
        this.label = label;
        this.height = height;
        this.width = width;
    }

    public static VideoResolution fromHeight(int height) {
        switch(height) {
            case 720:
                return HD_720;
            case 480:
                return SD_480;
            default:
                return HD_1080;
        }
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    @Override
    public String toString() {
        return label;
    }
}
