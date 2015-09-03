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
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class TransportStreamDecoder implements TivoStreamDecoder {
    private final TuringDecoder turingDecoder;
    private final int mpegOffset;
    private final CountingDataInputStream inputStream;
    private final OutputStream outputStream;
    private final Map<Integer, TransportStream> streams;

    private long packetCounter;

    public TransportStreamDecoder(TuringDecoder decoder, int mpegOffset, CountingDataInputStream inputStream,
                                  OutputStream outputStream) {
        this.turingDecoder = decoder;
        this.mpegOffset = mpegOffset;
        this.inputStream = inputStream;
        this.outputStream = outputStream;

        packetCounter = 0;
        streams = new HashMap<>();
        initPatStream();
    }

    private void initPatStream() {
        System.out.format("Creating new stream for PID (0x%04x)%n", 0);
        TransportStream stream = new TransportStream(0);
        streams.put(0, stream);
    }

    @Override
    public boolean process() {
        try {
            advanceToMpegOffset();
            TransportStreamPacket packet = new TransportStreamPacket();
            while (packet.readFrom(inputStream)) {
                packet.setPacketId(++packetCounter);
                System.out.println(packet);
                switch (packet.getPacketType()) {
                    case PROGRAM_ASSOCIATION_TABLE:
                        System.out.println("PAT packet");
                        break;
                    case AUDIO_VIDEO_PRIVATE_DATA:
                        System.out.println("A/V packet");
                        break;
                    default:
                        System.err.println("Unknown packet type");
                        return false;
                }
            }
            return true;
        } catch (IOException e) {
            System.err.format("Error reading transport stream: %s%n", e.getLocalizedMessage());
        }

        return false;
    }

    private void advanceToMpegOffset() throws IOException {
        int bytesToSkip = (int) (mpegOffset - inputStream.getPosition());
        if (bytesToSkip < 0) {
            System.err.format("Error: Transport stream advanced past MPEG data (MPEG at %d, current position = %d)%n",
                    mpegOffset, inputStream.getPosition());
        }
        inputStream.skipBytes(bytesToSkip);
    }
}
