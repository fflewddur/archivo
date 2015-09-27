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

package net.straylightlabs.hola.dns;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class DomainTest {
    @Test
    public void testDomainFromValidString() {
        Domain domain;
        domain = Domain.fromName("local");
        assertTrue("fromName(local) == local: " + domain.getName(), domain.getName().equals("local"));
        domain = Domain.fromName("local.");
        assertTrue("fromName(local.) == local: " + domain.getName(), domain.getName().equals("local"));
    }

    @Test
    public void testDomainFromStringWithService() {
        Domain domain;
        domain = Domain.fromName("_http._tcp.local");
        assertTrue("fromName(_http._tcp.local) == local: " + domain.getName(), domain.getName().equals("local"));
    }
}
