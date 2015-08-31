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

package net.straylightlabs.archivo.tivodecode;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class TivoStreamHeader {

    private char[] fileType;
    private int dummy04;
    private int dummy06;
    private int dummy08;
    private int mpegOffset;
    private int numChunks;
    private final InputStream inputStream;

    public TivoStreamHeader(InputStream inputStream) {
        fileType = new char[4];
        mpegOffset = 0;
        numChunks = 0;
        this.inputStream = inputStream;
    }

    public int getNumChunks() {
        return numChunks;
    }

    public boolean read() {
        try (DataInputStream input = new DataInputStream(inputStream)) {
            // First four bytes should be the characters "TiVo"
            for (int i = 0; i < 4; i++) {
                byte b = input.readByte();
                fileType[i] = (char) b;
            }
            // Next two bytes are a mystery
            dummy04 = input.readUnsignedShort();
            // Next two bytes tell us about the file's providence
            dummy06 = input.readUnsignedShort();
            // Next two bytes also remain a mystery
            dummy08 = input.readUnsignedShort();
            // Next four bytes are an unsigned int representing where the read MPEG data begins
            mpegOffset = input.readInt();
            // Next two bytes tell us how many TiVo-specific chunks of data are coming
            numChunks = input.readUnsignedShort();
        } catch (IOException e) {
            System.err.println("Error reading header: " + e.getLocalizedMessage());
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("[TiVo Header");
        sb.append(String.format(" fileType=%02X:%02X:%02X:%02X (%s)",
                (int) fileType[0], (int) fileType[1], (int) fileType[2], (int) fileType[3], new String(fileType)));
        sb.append(String.format(" dummy04=%04X", dummy04));
        sb.append(String.format(" dummy06=%04X", dummy06));
        sb.append(String.format(" dummy08=%04X", dummy08));
        sb.append(String.format(" mpegOffset=%d", mpegOffset));
        sb.append(String.format(" numChunks=%d", numChunks));
        sb.append("]");

        return sb.toString();
    }
}
