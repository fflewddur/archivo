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

import java.net.URL;

public class Channel {
    private final String name;
    private final String number;
    private final URL logoURL;

    public Channel(String name, String number, URL logoURL) {
        this.name = name;
        this.number = number;
        this.logoURL = logoURL;
    }

    public String getNumber() {
        return number;
    }

    public String getName() {
        return name;
    }

    public URL getLogoURL() {
        return logoURL;
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", name, number);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Channel)) {
            return false;
        }

        Channel channel = (Channel)o;
        return ((number.equals(channel.number)) && (name.equals(channel.name)));
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + number.hashCode();
        return result;
    }
}
