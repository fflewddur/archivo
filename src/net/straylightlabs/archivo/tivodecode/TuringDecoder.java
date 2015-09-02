/*
 * Java implementation of Turing encryption system for use with TiVo
 * Transport Streams. Based on TivoDecode 0.4.4 and Turning.h 1.4.
 *
 * Java port copyright 2015 Todd Kulesza <todd@dropline.net>.
 *
 * Copyright C 2002, Qualcomm Inc. Written by Greg Rose
 *
 * This software is free for commercial and non-commercial use subject to
 * the following conditions:
 *
 * 1.  Copyright remains vested in QUALCOMM Incorporated, and Copyright
 * notices in the code are not to be removed.  If this package is used in
 * a product, QUALCOMM should be given attribution as the author of the
 * Turing encryption algorithm. This can be in the form of a textual
 * message at program startup or in documentation (online or textual)
 * provided with the package.
 *
 * 2.  Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * a. Redistributions of source code must retain the copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * b. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the
 * distribution.
 *
 * c. All advertising materials mentioning features or use of this
 * software must display the following acknowledgement:  This product
 * includes software developed by QUALCOMM Incorporated.
 *
 * 3.  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE AND AGAINST
 * INFRINGEMENT ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * 4.  The license and distribution terms for any publically available version
 * or derivative of this code cannot be changed, that is, this code cannot
 * simply be copied and put under another distribution license including
 * the GNU Public License.
 *
 * 5.  The Turing family of encryption algorithms are covered by patents in
 * the United States of America and other countries. A free and
 * irrevocable license is hereby granted for the use of such patents to
 * the extent required to utilize the Turing family of encryption
 * algorithms for any purpose, subject to the condition that any
 * commercial product utilising any of the Turing family of encryption
 * algorithms should show the words "Encryption by QUALCOMM" either on the
 * product or in the associated documentation.
 */

package net.straylightlabs.archivo.tivodecode;

import org.apache.commons.codec.digest.DigestUtils;

import java.util.HashMap;
import java.util.Map;

public class TuringDecoder {
    private final byte[] key;
    private final Map<Integer, TuringStream> streams;

    private final static int SHORTENED_KEY_LENGTH = 17;

    public TuringDecoder(byte[] key) {
        this.key = key;
        streams = new HashMap<>();
    }

    public TuringStream prepareFrame(int streamId, int blockId) {
        TuringStream stream = streams.get(streamId);
        if (stream == null) {
            stream = new TuringStream(streamId, blockId);
            prepareFrameHelper(stream, streamId, blockId);
            streams.put(streamId, stream);
        }

        if (stream.getBlockId() != blockId) {
            prepareFrameHelper(stream, streamId, blockId);
        }

        return stream;
    }

    private void prepareFrameHelper(TuringStream stream, int streamId, int blockId) {
        // Update our key for the current stream and block
        key[16] = (byte) streamId;
        key[17] = (byte) ((blockId & 0xFF0000) >> 16);
        key[18] = (byte) ((blockId & 0x00FF00) >> 8);
        key[19] = (byte) (blockId & 0x0000FF);

        byte[] shortenedKey = new byte[SHORTENED_KEY_LENGTH];
        System.arraycopy(key, 0, shortenedKey, 0, SHORTENED_KEY_LENGTH);
        byte[] turkey = DigestUtils.sha1(shortenedKey);
        System.out.println("turkey: " + DigestUtils.sha1Hex(shortenedKey));
        byte[] turiv = DigestUtils.sha1(key);
        System.out.println("turiv: " + DigestUtils.sha1Hex(key));

        stream.reset(turkey, turiv);
    }

    public void skipBytes(TuringStream stream, int bytesToSkip) {
        if (stream.getCipherPos() + bytesToSkip < stream.getCipherLen()) {
            stream.setCipherPos(stream.getCipherPos() + bytesToSkip);
        } else {
            do {
                bytesToSkip -= stream.getCipherLen() - stream.getCipherPos();
                stream.generate();
            } while (bytesToSkip >= stream.getCipherLen());

            stream.setCipherPos(bytesToSkip);
        }
    }

    public void decryptBytes(TuringStream stream, byte[] buffer) {
        for (int i = 0; i < buffer.length; i++) {
            if (stream.getCipherPos() >= stream.getCipherLen()) {
                stream.generate();
            }
            byte b = stream.getCipherByte();
//            System.out.format("cipher pos: %d cipher len: %d cipher byte: %02x encoded byte: %02x ",
//                    stream.getCipherPos(), stream.getCipherLen(), b, buffer[i]);
            buffer[i] ^= b;
//            System.out.format("decoded byte: %02x%n", buffer[i]);
        }
    }
}
