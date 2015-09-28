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

public class Instance {
    private final InetAddress address;
    private final int port;

    public static Instance createFrom(Response response) {
        InetAddress address = response.getInetAddress();
        int port = response.getPort();

        return new Instance(address, port);
    }

    private Instance(InetAddress address, int port) {
        this.address = address;
        this.port = port;
    }
}
