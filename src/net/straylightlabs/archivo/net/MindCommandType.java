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

package net.straylightlabs.archivo.net;

public enum MindCommandType {
    UNKNOWN,
    AUTH,
    BODY_CONFIG_SEARCH,
    ID_SEARCH,
    RECORDING_FOLDER_ITEM_SEARCH,
    RECORDING_SEARCH,
    RECORDING_UPDATE;

    @Override public String toString() {
        // These strings must match the name of the command in the TiVo Mind API
        switch(this) {
            case AUTH:
                return "bodyAuthenticate";
            case BODY_CONFIG_SEARCH:
                return "bodyConfigSearch";
            case ID_SEARCH:
                return "idSearch";
            case RECORDING_FOLDER_ITEM_SEARCH:
                return "recordingFolderItemSearch";
            case RECORDING_SEARCH:
                return "recordingSearch";
            case RECORDING_UPDATE:
                return "recordingUpdate";
        }

        return "unknown";
    }
}
