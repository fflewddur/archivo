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

import org.apache.commons.codec.digest.DigestUtils;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class TivoStream {
    private TivoStreamHeader header;
    private TivoStreamChunk[] chunks;
    private TuringStream turing;
    private TuringStream metaTuring;

    private final String mak;
    private final InputStream inputStream;

    public TivoStream(InputStream inputStream, String mak) {
        this.inputStream = inputStream;
        this.mak = mak;
    }

    public boolean read() {
        try (DataInputStream dataInputStream = new DataInputStream(inputStream)) {
            header = new TivoStreamHeader(dataInputStream);
            if (!header.read()) {
                return false;
            }

            chunks = new TivoStreamChunk[header.getNumChunks()];
            for (int i = 0; i < header.getNumChunks(); i++) {
                chunks[i] = new TivoStreamChunk(dataInputStream);
                if (!chunks[i].read()) {
                    return false;
                }
                if (chunks[i].isEncrypted()) {

                } else {
                    turing = new TuringStream(chunks[i].getKey(mak));
                    metaTuring = new TuringStream(chunks[i].getMetadataKey(mak));
                }
            }
        } catch (IOException e) {
            System.err.format("Error reading TiVoStream file: %s%n", e.getLocalizedMessage());
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("[TivoStream");
        sb.append(String.format(" header=%s", header));
        for (int i = 0; i < header.getNumChunks(); i++) {
            sb.append(String.format(" chunk[%d]=%s", i, chunks[i]));
        }

        sb.append("]");

        return sb.toString();
    }

    private static class TivoStreamHeader {
        private char[] fileType;
        private int dummy04;
        private int dummy06;
        private int dummy08;
        private int mpegOffset;
        private int numChunks;
        private final DataInputStream input;

        public TivoStreamHeader(DataInputStream inputStream) {
            fileType = new char[4];
            mpegOffset = 0;
            numChunks = 0;
            this.input = inputStream;
        }

        public int getNumChunks() {
            return numChunks;
        }

        public boolean read() {
            try {
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
            sb.append(String.format(" fileType=%s (%02X:%02X:%02X:%02X)",
                    new String(fileType), (int) fileType[0], (int) fileType[1], (int) fileType[2], (int) fileType[3]));
            sb.append(String.format(" dummy04=%04X", dummy04));
            sb.append(String.format(" dummy06=%04X", dummy06));
            sb.append(String.format(" dummy08=%04X", dummy08));
            sb.append(String.format(" mpegOffset=%d", mpegOffset));
            sb.append(String.format(" numChunks=%d", numChunks));
            sb.append("]");

            return sb.toString();
        }
    }

    private static class TivoStreamChunk {
        private int chunkSize;
        private int dataSize;
        private int id;
        private ChunkType type;
        private byte[] data;
        private final DataInputStream inputStream;

        private final static int CHUNK_HEADER_SIZE = 12; // Size of each chunk's header, in bytes
        private final static String MEDIA_MAK_PREFIX = "tivo:TiVo DVR:";
        private final static String LOOKUP_STRING = "0123456789abcdef";

        public TivoStreamChunk(DataInputStream inputStream) {
            this.inputStream = inputStream;
        }

        public boolean isEncrypted() {
            return type == ChunkType.ENCRYPTED;
        }

        public boolean read() {
            try {
                // First four bytes tell us this chunk's size
                chunkSize = inputStream.readInt();
                // The next four bytes tell us the length of the chunk's payload
                dataSize = inputStream.readInt();
                // Two bytes for the chunk's ID
                id = inputStream.readUnsignedShort();
                // Two bytes for the type of payload
                type = ChunkType.valueOf(inputStream.readUnsignedShort());

                // The rest is the payload
                data = new byte[dataSize];

                for (int totalBytesRead = 0, bytesRead = 0; totalBytesRead < dataSize && bytesRead != -1; totalBytesRead += bytesRead) {
                    bytesRead = inputStream.read(data);
                }

                // There might be padding bytes at the end of the chunk
                int paddingBytes = chunkSize - dataSize - CHUNK_HEADER_SIZE;
                for (int i = 0; i < paddingBytes; i++) {
                    inputStream.readByte();
                }
            } catch (IOException e) {
                System.err.println("Error reading chunk: " + e.getLocalizedMessage());
                return false;
            }

            return true;
        }

        public byte[] getKey(String mak) {
            byte[] makBytes = mak.getBytes();
            byte[] bytes = new byte[makBytes.length + dataSize];
            System.arraycopy(makBytes, 0, bytes, 0, makBytes.length);
            System.arraycopy(data, 0, bytes, makBytes.length, dataSize);
            return DigestUtils.sha1(bytes);
        }

        public byte[] getMetadataKey(String mak) {
            byte[] prefixBytes = MEDIA_MAK_PREFIX.getBytes();
            byte[] makBytes = mak.getBytes();
            byte[] bytes = new byte[prefixBytes.length + makBytes.length];
            System.arraycopy(prefixBytes, 0, bytes, 0, prefixBytes.length);
            System.arraycopy(makBytes, 0, bytes, prefixBytes.length, makBytes.length);
            byte[] md5 = DigestUtils.md5(bytes);

            byte[] metaKey = new byte[32];
            for (int i = 0; i < md5.length; i++) {
                metaKey[i * 2] = (byte) LOOKUP_STRING.charAt((md5[i] >> 4) & 0xf);
                metaKey[i * 2 + 1] = (byte) LOOKUP_STRING.charAt(md5[i] & 0xf);
            }
            return getKey(new String(metaKey));
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            sb.append("([TiVo Chunk");
            sb.append(String.format(" chunkSize=%d", chunkSize));
            sb.append(String.format(" dataSize=%d", dataSize));
            sb.append(String.format(" id=%d", id));
            sb.append(String.format(" type=%s", type));
            sb.append("]");

            return sb.toString();
        }

        public enum ChunkType {
            PLAINTEXT,
            ENCRYPTED;

            public static ChunkType valueOf(int val) {
                switch (val) {
                    case 0:
                        return PLAINTEXT;
                    case 1:
                        return ENCRYPTED;
                }
                throw new IllegalArgumentException(String.format("%d is an unsupported chunk type", val));
            }
        }
    }

    public enum StreamType {
        PROGRAM,
        TRANSPORT
    }
}
