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

import java.nio.ByteBuffer;

public abstract class Message {
    protected final ByteBuffer buffer;

    public final static int MAX_LENGTH = 9000; // max size of mDNS packets, in bytes
    public final static int HEADER_LENGTH = 12; // DNS headers are 12 bytes

    private final static int USHORT_MASK = 0xFFFF;

    protected Message() {
        buffer = ByteBuffer.allocate(MAX_LENGTH);
    }

    protected int readUnsignedShort() {
        return buffer.getShort() & USHORT_MASK;
    }

//    protected void buildHeader() {
//        if (buffer.position() != 0) {
//            throw new IllegalStateException("buildHeader must be called before any other buffer operations");
//        }
//    }
//
//    protected void parseHeader() {
//        if (buffer.position() != 0) {
//            throw new IllegalStateException("parseHeader must be called before any other buffer operations");
//        }
//    }

    public String dumpBuffer() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < buffer.position(); i++) {
            sb.append(String.format("%02x", buffer.get(i)));
            if ((i + 1) % 8 == 0) {
                sb.append('\n');
            } else if ((i + 1) % 2 == 0) {
                sb.append(' ');
            }
        }
        return sb.toString();
    }
}
