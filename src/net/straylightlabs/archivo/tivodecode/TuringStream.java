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


import java.util.Arrays;

public class TuringStream {
    private int streamId;
    private int blockId;
    private int cipherPos;
    private int cipherLen;
    private byte[] cipherData;
    private TuringReferenceImp turingReferenceImp;

    public TuringStream(int streamId, int blockId) {
        this.streamId = streamId;
        this.blockId = blockId;
        cipherData = new byte[TuringReferenceImp.MAXSTREAM + Long.BYTES];
        turingReferenceImp = new TuringReferenceImp();
    }

    public int getStreamId() {
        return streamId;
    }

    public int getBlockId() {
        return blockId;
    }

    public int getCipherPos() {
        return cipherPos;
    }

    public void setCipherPos(int val) {
        cipherPos = val;
    }

    public int getCipherLen() {
        return cipherLen;
    }

    /**
     * Return the byte at the current cipher position, then increment the position.
     */
    public byte getCipherByte() {
        return cipherData[cipherPos++];
    }

    public void generate() {
        cipherLen = turingReferenceImp.turingGen(cipherData);
        cipherPos = 0;
    }

    public void reset(byte[] turkey, byte[] turiv) {
        cipherPos = 0;
        turingReferenceImp.setTuringKey(turkey, 20);
        turingReferenceImp.setTuringIV(turiv, 20);
        Arrays.fill(cipherData, (byte) 0);
        cipherLen = turingReferenceImp.turingGen(cipherData);
    }

    private void dumpCipherData() {
        StringBuilder sb = new StringBuilder();
        int bytesPerBlock = 1;
        int blocksPerLine = 16;
        for (int i = 0; i < cipherLen; i++) {
            sb.append(String.format("%02X", cipherData[i]));
            if ((i + 1) % (blocksPerLine * bytesPerBlock) == 0) {
                sb.append("\n");
            } else if ((i + 1) % bytesPerBlock == 0)
                sb.append(" ");

        }
        System.out.println(sb.toString());
    }
}
