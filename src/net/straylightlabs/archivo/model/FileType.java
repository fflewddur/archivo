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
    TIVO("Encrypted TiVo Files", "*.TiVo"),
    MPEG("MPEG Files", "*.mpg");

    private String description;
    private String extension;

    FileType(String description, String extension) {
        this.description = description;
        this.extension = extension;
    }

    public static FileType fromExtension(String extension) {
        for (FileType ft : values()) {
            if (ft.extension.equalsIgnoreCase(extension)) {
                return ft;
            }
        }
        throw new IllegalArgumentException("Unknown extension: " + extension);
    }

    public static FileType getDefault() {
        return MPEG;
    }

    public String getDescription() {
        return description;
    }

    public String getExtension() {
        return extension;
    }
}
