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

package net.straylightlabs.hola.sd;

import net.straylightlabs.hola.dns.Response;

import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Instance {
    private final String name;
    private final List<InetAddress> addresses;
    private final int port;
    private final Map<String, String> attributes;

    public static Instance createFrom(Response response) {
        String name = response.getUserVisibleName();
        List<InetAddress> addresses = response.getInetAddresses();
        int port = response.getPort();
        Map<String, String> attributes = response.getAttributes();

        return new Instance(name, addresses, port, attributes);
    }

    private Instance(String name, List<InetAddress> addresses, int port, Map<String, String> attributes) {
        this.name = name;
        this.addresses = addresses;
        this.port = port;
        this.attributes = attributes;
    }

    public String getName() {
        return name;
    }

    public List<InetAddress> getAddresses() {
        return Collections.unmodifiableList(addresses);
    }

    public int getPort() {
        return port;
    }

    public boolean hasAttribute(String attribute) {
        return attributes.containsKey(attribute);
    }

    public String lookupAttribute(String attribute) {
        return attributes.get(attribute);
    }

    @Override
    public String toString() {
        return "Instance{" +
                "name='" + name + '\'' +
                ", addresses=" + addresses +
                ", port=" + port +
                '}';
    }
}
