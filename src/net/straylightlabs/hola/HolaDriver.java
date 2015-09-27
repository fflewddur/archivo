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

package net.straylightlabs.hola;

import net.straylightlabs.archivo.Archivo;
import net.straylightlabs.hola.dns.Domain;
import net.straylightlabs.hola.sd.Service;
import net.straylightlabs.hola.sd.ServiceQuery;

import java.io.IOException;
import java.net.UnknownHostException;

/**
 * A minimal implementation of mDNS-SD, as described in RFCs 6762 & 6763.
 */

// TODO Add Instance class to model devices

public class HolaDriver {
    public static void main(String[] args) {
        try {
            Service service = Service.fromName("_tivo-mindrpc._tcp");
            ServiceQuery query = ServiceQuery.createFor(service, Domain.LOCAL);
            query.runOnce();
        } catch (UnknownHostException e) {
            Archivo.logger.error("Unknown host: ", e);
        } catch (IOException e) {
            Archivo.logger.error("IO error: ", e);
        }
    }
}
