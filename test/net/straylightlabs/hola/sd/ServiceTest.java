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

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ServiceTest {
    @Test
    public void testServiceFromValidString() {
        Service service;
        service = Service.fromName("_tcp");
        assertTrue("fromName(_tcp) == _tcp: " + service.getName(), service.getName().equals("_tcp"));
        service = Service.fromName("_tcp.");
        assertTrue("fromName(_tcp.) == _tcp: " + service.getName(), service.getName().equals("_tcp"));
        service = Service.fromName("_udp");
        assertTrue("fromName(_udp) == _udp: " + service.getName(), service.getName().equals("_udp"));
        service = Service.fromName("_udp.");
        assertTrue("fromName(_udp.) == _udp: " + service.getName(), service.getName().equals("_udp"));
        service = Service.fromName("_http._tcp");
        assertTrue("fromName(_http._tcp) == _http._tcp: " + service.getName(), service.getName().equals("_http._tcp"));
        service = Service.fromName("_http._tcp.");
        assertTrue("fromName(_http._tcp.) == _http._tcp: " + service.getName(), service.getName().equals("_http._tcp"));
    }

    @Test
    public void testServiceFromStringWithDomain() {
        Service service;
        service = Service.fromName("_http._tcp.local.");
        assertTrue("fromName(_http._tcp.local.) = _http._tcp", service.getName().equals("_http._tcp"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testServiceFromInvalidString() {
        Service.fromName("invalidname._tcp");
    }
}
