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
import net.straylightlabs.hola.dns.Question;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;

public class ServiceQuery {
    private final Service service;
    private final Domain domain;

    public static final String MDNS_IP4_ADDRESS = "224.0.0.251";
    public static final int MDNS_PORT = 5353;

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

        MulticastSocket socket = new MulticastSocket();
        socket.setReuseAddress(true);

        question.askOn(socket);

        while (true) {
            byte[] responseBuffer = new byte[1024];
            DatagramPacket response = new DatagramPacket(responseBuffer, responseBuffer.length);
            Archivo.logger.info("Waiting for response...");
            socket.receive(response);
            Archivo.logger.info("response received!");

            Archivo.logger.info("length = {}", response.getLength());
            for (int i = 0; i < response.getLength(); i++) {
                System.out.format("%02x ", responseBuffer[i]);
                if ((i + 1) % 8 == 0) {
                    System.out.println();
                }
            }

            for (int i = 12; i < response.getLength(); i++) {
                int len = responseBuffer[i] & 0xff;
                System.out.print("Len = " + len + ": ");
                for (int j = 1; j <= len; j++) {
                    System.out.print((char) responseBuffer[i + j]);
                }
                i += len;
                System.out.println();
                if (len == 0) {
                    break;
                }
            }
        }
    }
}
