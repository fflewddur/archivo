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

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.*;

public class TransportStream {
    private final int pid;
    private int streamId;
    private StreamType type;
    private byte[] pesBuffer;
    private byte[] turingKey;
    private int turingBlockNumber;
    private final Deque<TransportStreamPacket> packets;
    private final Deque<Integer> pesHeaderLengths;
    private final OutputStream outputStream;

    public static final int TS_FRAME_SIZE = 188;

    public TransportStream(int pid, OutputStream outputStream) {
        this.pid = pid;
        this.type = StreamType.NONE;
        this.outputStream = outputStream;
        pesBuffer = new byte[TS_FRAME_SIZE * 10];
        packets = new ArrayDeque<>();
        pesHeaderLengths = new ArrayDeque<>();
    }

    public TransportStream(int pid, OutputStream outputStream, StreamType type) {
        this(pid, outputStream);
        this.type = type;
    }

    public void setStreamId(int val) {
        streamId = val;
    }

    public StreamType getType() {
        return type;
    }

    public void setKey(byte[] val) {
        turingKey = val;
    }

    public boolean addPacket(TransportStreamPacket packet) {
        boolean flushBuffers = false;

        // If this packet's Payload Unit Start Indicator is set,
        // or one of the stream's previous packet's was set, we
        // need to buffer the packet, such that we can make an
        // attempt to determine where the end of the PES headers
        // lies.   Only after we've done that, can we determine
        // the packet offset at which decryption is to occur.
        // The accounts for the situation where the PES headers
        // straddles two packets, and decryption is needed on the 2nd.
        if (packet.isPayloadStart() || packets.size() != 0) {
            packets.addLast(packet);

            // Form one contiguous buffer containing all buffered packet payloads
            Arrays.fill(pesBuffer, (byte) 0);
            int pesBufferLen = 0;
            for (TransportStreamPacket p : packets) {
                ByteBuffer data = p.getData();
                data.get(pesBuffer, pesBufferLen, data.capacity());
                pesBufferLen += data.capacity();
            }

            // Scan the contiguous buffer for PES headers
            // in order to find the end of PES headers.
            pesHeaderLengths.clear();
            if (!getPesHeaderLength(pesBuffer)) {
                System.err.format("Failed to parse PES headers for packet %d%n", packet.getPacketId());
                return false;
            }
            int pesHeaderLength = pesHeaderLengths.stream().mapToInt(i -> i).sum() / 8;

        } else {
            flushBuffers = true;
            packets.addLast(packet);
        }

        return true;
    }

    private boolean getPesHeaderLength(byte[] buffer) {
        MpegParser parser = new MpegParser(buffer);
        boolean done = false;
        while (!done && !parser.isEOF()) {
            parser.advanceBits(8); // Skip header offset
            if (0x000001 != parser.nextBits(24)) {
                done = true;
                continue;
            }

            int len = 0;
            int startCode = parser.nextBits(32);
            parser.clear();
            switch (MpegParser.ControlCode.valueOf(startCode)) {
                case EXTENSION_START_CODE:
                    len = parser.extensionHeader();
                    break;
                case GROUP_START_CODE:
                    len = parser.groupOfPicturesHeader();
                    break;
                case USER_DATA_START_CODE:
                    len = parser.userData();
                    break;
                case PICTURE_START_CODE:
                    len = parser.pictureHeader();
                    break;
                case SEQUENCE_HEADER_CODE:
                    len = parser.sequenceHeader();
                    break;
                case SEQUENCE_END_CODE:
                    len = parser.sequenceEnd();
                    break;
                case ANCILLARY_DATA_CODE:
                    len = parser.ancillaryData();
                    break;
                default:
                    if (startCode >= 0x101 && startCode <= 0x1AF) {
                        done = true;
                    } else if ((startCode == 0x1BD) || (startCode >= 0x1C0 && startCode <= 0x1EF)) {
                        len = parser.pesHeader();
                    } else {
                        System.err.format("Error: Unhandled PES header: 0x%08x%n", startCode);
                        return false;
                    }
            }

            if (len > 0) {
                pesHeaderLengths.addLast(len);
            }
        }

        return true;
    }

    public enum StreamType {
        AUDIO,
        VIDEO,
        PRIVATE_DATA,
        OTHER,
        NONE;

        private static Map<Integer, StreamType> typeMap;

        static {
            typeMap = new HashMap<>();
            typeMap.put(0x01, VIDEO);
            typeMap.put(0x02, VIDEO);
            typeMap.put(0x10, VIDEO);
            typeMap.put(0x1b, VIDEO);
            typeMap.put(0x80, VIDEO);
            typeMap.put(0xea, VIDEO);

            typeMap.put(0x03, AUDIO);
            typeMap.put(0x04, AUDIO);
            typeMap.put(0x11, AUDIO);
            typeMap.put(0x0f, AUDIO);
            typeMap.put(0x81, AUDIO);
            typeMap.put(0x8a, AUDIO);

            typeMap.put(0x08, OTHER);
            typeMap.put(0x0a, OTHER);
            typeMap.put(0x0b, OTHER);
            typeMap.put(0x0c, OTHER);
            typeMap.put(0x0d, OTHER);
            typeMap.put(0x14, OTHER);
            typeMap.put(0x15, OTHER);
            typeMap.put(0x16, OTHER);
            typeMap.put(0x17, OTHER);
            typeMap.put(0x18, OTHER);
            typeMap.put(0x19, OTHER);

            typeMap.put(0x05, OTHER);
            typeMap.put(0x06, OTHER);
            typeMap.put(0x07, OTHER);
            typeMap.put(0x09, OTHER);
            typeMap.put(0x0e, OTHER);
            typeMap.put(0x12, OTHER);
            typeMap.put(0x13, OTHER);
            typeMap.put(0x1a, OTHER);
            typeMap.put(0x7f, OTHER);

            typeMap.put(0x97, PRIVATE_DATA);

            typeMap.put(0x00, NONE);
        }

        public static StreamType valueOf(int val) {
            return typeMap.getOrDefault(val, PRIVATE_DATA);
        }
    }
}
