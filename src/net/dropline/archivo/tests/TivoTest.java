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

package net.dropline.archivo.tests;

import net.dropline.archivo.model.Tivo;
import org.junit.Test;


import static org.junit.Assert.assertEquals;

public class TivoTest {
    @Test
    public void testEquality() {
        Tivo a = new Tivo.Builder().name("Foo").tsn("Bar").build();
        Tivo aCopy = new Tivo.Builder().name("Foo").tsn("Bar").build();
        Tivo b = new Tivo.Builder().name("Foo2").tsn("Bar2").build();

        assertEquals("a equals aCopy", true, a.equals(aCopy));
        assertEquals("a not equals b", false, a.equals(b));
        assertEquals("aCopy not equals b", false, aCopy.equals(b));
    }
}