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
 *
 */

package net.straylightlabs.archivo.tivodecode;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

/**

 */
public class TivoDecoder {
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private final String mak;

    public final static String QUALCOMM_MSG = "Encryption by QUALCOMM ;)";

    public TivoDecoder(InputStream inputStream, OutputStream outputStream, String mak) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.mak = mak;
    }

    public boolean decode() {
        System.out.format("%s%n%n", QUALCOMM_MSG);

        TivoStream stream = new TivoStream(inputStream, outputStream, mak);
        stream.process();
//        System.out.println(stream);
//            stream.printChunkPayloads();

        return false;
    }


    /**
     * To enable easier testing.
     *
     * @param args Paths to the input and output files and a string representing the MAK, in that order
     */
    public static void main(String[] args) {
        Path in = Paths.get(args[0]);
        Path out = Paths.get(args[1]);
        String mak = args[2];
        try (FileInputStream inputStream = new FileInputStream(in.toFile());
             BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(out.toFile()))) {
            TivoDecoder decoder = new TivoDecoder(inputStream, outputStream, mak);
            decoder.decode();
        } catch (FileNotFoundException e) {
            System.err.format("Error: %s%n", e.getLocalizedMessage());
        } catch (IOException e) {
            System.err.format("Error reading/writing files: %s%n", e.getLocalizedMessage());
        }
    }
}
