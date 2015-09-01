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

/**
 * Wrap a DataInputStream and include a marker of the current position.
 */
public class CountingDataInputStream implements AutoCloseable {
    private final DataInputStream stream;
    private long position; // Current position in @stream, in bytes.

    public CountingDataInputStream(InputStream stream) {
        this.stream = new DataInputStream(stream);
        position = 0;
    }

    public long getPosition() {
        return position;
    }

    public int read(byte[] buffer) throws IOException {
        int val = stream.read(buffer);
        position += val;
        return val;
    }

    public byte readByte() throws IOException {
        byte val = stream.readByte();
        position += Byte.BYTES;
        return val;
    }

    public int readInt() throws IOException {
        int val = stream.readInt();
        position += Integer.BYTES;
        return val;
    }

    public int readUnsignedShort() throws IOException {
        int val = stream.readUnsignedShort();
        position += Short.BYTES;
        return val;
    }

    public int skipBytes(int bytesToSkip) throws IOException {
        int val = stream.skipBytes(bytesToSkip);
        position += bytesToSkip;
        return val;
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }
}
