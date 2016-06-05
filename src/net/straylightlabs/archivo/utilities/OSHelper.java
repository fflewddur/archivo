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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OSHelper {
    private static final String osName;
    private static final Runtime runtime;
    private static int cpuCores;
    private static int cpuThreads;
    private static String exeSuffix;

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

    public static int getProcessorCores() {
        if (cpuCores == 0) {
            try {
                if (isWindows()) {
                    cpuCores = getProcessorCoresWindows();
                } else if (isMacOS()) {
                    cpuCores = getProcessorCoresMacOSX();
                }
            } catch (NumberFormatException e) {
                logger.error("Invalid number of CPU cores: {}", e.getLocalizedMessage(), e);
            }

            // TODO add Linux support
            // Fallback to single-threaded operation if we can't figure out the number of available cores
            if (cpuCores == 0) {
                cpuCores = 1;
            }
        }

        return cpuCores;
    }

    private static int getProcessorCoresWindows() {
        int cores = 1;
        String[] cmd = {"wmic", "cpu", "get", "NumberOfCores", "/Format:List"};
        try {
            ProcessBuilder builder = new ProcessBuilder().command(cmd).redirectErrorStream(true);
            Process process = builder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            Pattern pattern = Pattern.compile(".*NumberOfCores=(\\d+).*");
            for (String output = reader.readLine(); output != null; output = reader.readLine()) {
                if (output.isEmpty()) {
                    continue;
                }

                Matcher matcher = pattern.matcher(output);
                if (matcher.matches()) {
                    cores = Integer.parseInt(matcher.group(1));
                }
            }
            reader.close();
        } catch (IOException e) {
            logger.error("Error calculating number of processor cores: {}", e.getLocalizedMessage());
        }
        return cores;
    }

    private static int getProcessorCoresMacOSX() {
        int cores = 1;
        String[] cmd = {"sysctl", "-n", "hw.physicalcpu"};
        try {
            ProcessBuilder builder = new ProcessBuilder().command(cmd).redirectErrorStream(true);
            Process process = builder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String output = reader.readLine();
            if (output != null) {
                cores = Integer.parseInt(output);
            }
        } catch (IOException e) {
            logger.error("Error calculating number of processor cores: {}", e.getLocalizedMessage());
        }
        return cores;
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
