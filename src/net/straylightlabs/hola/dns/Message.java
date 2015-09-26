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

    protected final static int BUFFER_SIZE = 1024 * 10; // 10 KB

    protected Message() {
        buffer = ByteBuffer.allocate(BUFFER_SIZE);
    }

    protected void buildHeader() {
        if (buffer.position() != 0) {
            throw new IllegalStateException("buildHeader must be called before any other buffer operations");
        }
    }

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
