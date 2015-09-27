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

package net.straylightlabs.hola.sd;

import net.straylightlabs.archivo.Archivo;
import net.straylightlabs.hola.dns.Domain;
import net.straylightlabs.hola.dns.Message;
import net.straylightlabs.hola.dns.Question;
import net.straylightlabs.hola.dns.Response;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;

public class ServiceQuery {
    private final Service service;
    private final Domain domain;
    private MulticastSocket socket;

    public static final String MDNS_IP4_ADDRESS = "224.0.0.251";
    public static final int MDNS_PORT = 5353;

    /**
     * The browsing socket will timeout after this many milliseconds
     */
    private static final int BROWSING_TIMEOUT = 250;
    /**
     * Stop browsing after this many consecutive timeouts
     */
    private static final int MAX_BROWSING_TIMEOUTS = 4;

    public static ServiceQuery createFor(Service service, Domain domain) {
        return new ServiceQuery(service, domain);
    }

    private ServiceQuery(Service service, Domain domain) {
        this.service = service;
        this.domain = domain;
    }

    public void browse() throws IOException {
        Question question = new Question(service, domain);
        System.out.println("Service = " + service);
        System.out.println("Domain = " + domain);
        System.out.println("Question = \n" + question.dumpBuffer());

        try {
            openSocket();
            question.askOn(socket);
            collectResponses();
        } finally {
            closeSocket();
        }

    }

    private void openSocket() throws IOException {
        socket = new MulticastSocket();
        socket.setReuseAddress(true);
        socket.setSoTimeout(BROWSING_TIMEOUT);
    }

    private void collectResponses() throws IOException {
        for (int timeouts = 0; timeouts < MAX_BROWSING_TIMEOUTS; ) {
            byte[] responseBuffer = new byte[Message.MAX_LENGTH];
            DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
            Archivo.logger.info("Waiting for response...");
            try {
                socket.receive(responsePacket);
                Response response = Response.createFrom(responsePacket);
                System.out.println("Response received: " + response);
                timeouts = 0;
            } catch (SocketTimeoutException e) {
                timeouts++;
//                continue;
            }

//            Archivo.logger.info("response received!");
//
//            Archivo.logger.info("length = {}", responsePacket.getLength());
//            for (int i = 0; i < responsePacket.getLength(); i++) {
//                System.out.format("%02x ", responseBuffer[i]);
//                if ((i + 1) % 8 == 0) {
//                    System.out.println();
//                }
//            }
//
//            for (int i = 12; i < responsePacket.getLength(); i++) {
//                int len = responseBuffer[i] & 0xff;
//                System.out.print("Len = " + len + ": ");
//                for (int j = 1; j <= len; j++) {
//                    System.out.print((char) responseBuffer[i + j]);
//                }
//                i += len;
//                System.out.println();
//                if (len == 0) {
//                    break;
//                }
//            }
        }
    }

    private void closeSocket() {
        if (socket != null) {
            socket.close();
            socket = null;
        }
    }
}
