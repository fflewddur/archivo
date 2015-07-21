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

import net.dropline.archivo.MainApp;
import net.dropline.archivo.model.Recording;
import net.dropline.archivo.model.Tivo;
import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

public class TivoTest {
    @Test
    public void testCreateTivoConnection() throws UnknownHostException {
        InetAddress address = InetAddress.getByAddress(MainApp.testDeviceAddress);
        Tivo tc = new Tivo("TiVo", address, MainApp.testDeviceMAK);
    }

    @Test
    public void testGetRecordings() throws IOException {
        InetAddress address = InetAddress.getByAddress(MainApp.testDeviceAddress);
        Tivo tc = new Tivo("TiVo", address, MainApp.testDeviceMAK);
        List<Recording> recordings = tc.getRecordings();
    }
}