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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SoftwareUpdateDetails {
    private final String version;
    private final List<String> notableChanges;
    private final URL location;
    private final LocalDate releaseDate;
    private final boolean isAvailable;

    public final static Logger logger = LoggerFactory.getLogger(SoftwareUpdateDetails.class);

    public static final SoftwareUpdateDetails UNAVAILABLE = new SoftwareUpdateDetails();

    private SoftwareUpdateDetails() {
        version = null;
        notableChanges = Collections.emptyList();
        location = null;
        releaseDate = null;
        isAvailable = false;
    }

    public SoftwareUpdateDetails(String version, URL location, LocalDate releaseDate, List<String> changes) {
        this.version = version;
        this.notableChanges = new ArrayList<>(changes);
        this.location = location;
        this.releaseDate = releaseDate;
        this.isAvailable = true;
    }

    public String getVersion() {
        return version;
    }

    public String getSummary() {
        List<String> distinctChanges = notableChanges.stream().distinct().collect(Collectors.toList());
        StringBuilder sb = new StringBuilder();
        int length = distinctChanges.size();
        for (int i = 0; i < length; i++) {
            sb.append(distinctChanges.get(i));
            if (length > 2 && i + 2 <= length) {
                sb.append(", ");
            }
            if (i + 2 == length) {
                sb.append(" and ");
            }
        }
        return sb.toString().replace("  ", " ");
    }

    public URL getLocation() {
        return location;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    @Override
    public String toString() {
        return "SoftwareUpdateDetails{" +
                "version='" + version + '\'' +
                ", summary='" + getSummary() + '\'' +
                ", location=" + location +
                ", releaseDate=" + releaseDate +
                ", isAvailable=" + isAvailable +
                '}';
    }

    public static String versionToString(int major, int minor, int release, boolean isBeta, int betaNumber) {
        if (isBeta) {
            return String.format("%d.%d.%d Beta %d", major, minor, release, betaNumber);
        } else {
            return String.format("%d.%d.%d", major, minor, release);
        }
    }
}
