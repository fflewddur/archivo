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

package net.straylightlabs.archivo.utilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

public class OSHelper {
    private static final String osName;
    private static final Runtime runtime;
    private static int cpuThreads;
    private static Boolean isAMD64;
    private static String exeSuffix;

    @SuppressWarnings("unused")
    private final static Logger logger = LoggerFactory.getLogger(OSHelper.class);

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
        if (exeSuffix == null) {
            if (isWindows()) {
                exeSuffix = ".exe";
            } else {
                exeSuffix = "";
            }
        }
        return exeSuffix;
    }

    public static String getArchSuffix() {
        if (isWindows()) {
            if (isAMD64()) {
                return "-64";
            } else {
                return "-32";
            }
        } else {
            return "";
        }
    }

    private static boolean isAMD64() {
        if (isAMD64 == null) {
            String arch = System.getenv("PROCESSOR_ARCHITECTURE");
            isAMD64 = arch.equalsIgnoreCase("amd64");
        }
        return isAMD64;
    }

    public static int getProcessorThreads() {
        if (cpuThreads == 0) {
            cpuThreads = runtime.availableProcessors();
        }
        return cpuThreads;
    }

    @SuppressWarnings("unused")
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

    public static String getFileBrowserName() {
        if (isWindows()) {
            return "File Explorer";
        } else if (isMacOS()) {
            return "Finder";
        } else {
            return "File Browser";
        }
    }
}
