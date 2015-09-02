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

import java.io.IOException;
import java.io.InputStream;

public class TivoStream {
    private TivoStreamHeader header;
    private TivoStreamChunk[] chunks;
    private TuringDecoder decoder;
    private TuringDecoder metaDecoder;
    private long metaPosition;

    private final String mak;
    private final InputStream inputStream;

    public TivoStream(InputStream inputStream, String mak) {
        this.inputStream = inputStream;
        this.mak = mak;
        metaPosition = 0;
    }

    public boolean read() {
        try (CountingDataInputStream dataInputStream = new CountingDataInputStream(inputStream)) {
            header = new TivoStreamHeader(dataInputStream);
            if (!header.read()) {
                return false;
            }

            chunks = new TivoStreamChunk[header.getNumChunks()];
            for (int i = 0; i < header.getNumChunks(); i++) {
                long chunkDataPos = dataInputStream.getPosition() + TivoStreamChunk.CHUNK_HEADER_SIZE;
                chunks[i] = new TivoStreamChunk(dataInputStream);
                if (!chunks[i].read()) {
                    return false;
                }
                if (chunks[i].isEncrypted()) {
                    int offset = (int) (chunkDataPos - metaPosition);
                    chunks[i].decryptMetadata(metaDecoder, offset);
                    metaPosition = chunkDataPos + chunks[i].getDataSize();
                } else {
                    decoder = new TuringDecoder(chunks[i].getKey(mak));
                    metaDecoder = new TuringDecoder(chunks[i].getMetadataKey(mak));
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

    public void printChunkPayloads() {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < header.getNumChunks(); i++) {
            sb.append(String.format("<START OF CHUNK %d PAYLOAD>%s<END OF PAYLOAD>%n", i, chunks[i].getDataString()));
        }

        System.out.print(sb.toString());
    }

    public enum StreamType {
        PROGRAM,
        TRANSPORT
    }
}
