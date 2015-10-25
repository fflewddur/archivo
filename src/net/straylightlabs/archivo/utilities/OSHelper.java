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

package net.straylightlabs.archivo.utilities;

import java.nio.file.Path;
import java.nio.file.Paths;

public class OSHelper {
    private static final String osName;
    private static final Runtime runtime;

    static {
        osName = System.getProperty("os.name").toLowerCase();
        runtime = Runtime.getRuntime();
    }

    public static boolean isWindows() {
        return osName.startsWith("windows");
    }

    public static boolean isMacOS() {
        return osName.startsWith("mac os");
    }

    public static String getExeSuffix() {
        if (isWindows()) {
            return ".exe";
        } else {
            return "";
        }
    }

    public static int getProcessorCores() {
        return runtime.availableProcessors();
    }

    public static Path getApplicationDirectory() {
        if (isWindows()) {
            String programFilesEnv = System.getenv("ProgramFiles");
            if (programFilesEnv != null) {
                return Paths.get(programFilesEnv);
            } else {
                return Paths.get("C:");
            }
        } else {
            return Paths.get("/");
        }
    }

    public static Path getDataDirectory() {
        Path dataDir;

        if (isWindows()) {
            dataDir = Paths.get(System.getenv("APPDATA"), "Archivo");
        } else if (isMacOS()) {
            dataDir = Paths.get(System.getProperty("user.home"), "Library", "Application Support", "Archivo");
        } else {
            dataDir = Paths.get(System.getProperty("user.home"), ".archivo");
        }

        return dataDir;
    }
}
