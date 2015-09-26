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

import net.straylightlabs.hola.sd.Service;
import net.straylightlabs.hola.sd.ServiceQuery;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

public class Question extends Message {
    private final Service service;
    private final Domain domain;

    private final static short UNICAST_RESPONSE_BIT = (short) 0x8000;

    public Question(Service service, Domain domain) {
        super();
        this.service = service;
        this.domain = domain;
        build();
    }

    private void build() {
        buildHeader();

        // QNAME
        service.getLabels().forEach(this::addLabelToBuffer);
        domain.getLabels().forEach(this::addLabelToBuffer);
        addLabelToBuffer("");

        // QTYPE
        buffer.putShort(Record.Type.PTR.asShort());

        // QCLASS
        // FIXME Only set unicast bit for initial queries
        short qclass = (short) (Record.Class.IN.asShort() | UNICAST_RESPONSE_BIT);
        buffer.putShort(qclass);
    }

    private void addLabelToBuffer(String label) {
        byte[] labelBytes = label.getBytes();
        buffer.put((byte) (labelBytes.length & 0xff));
        buffer.put(labelBytes);
    }

    protected void buildHeader() {
        super.buildHeader();
        buffer.putShort((short) 0x0); // ID should be 0
        buffer.put((byte) 0x0);
        buffer.put((byte) 0x0);
        buffer.putShort((short) 0x1); // 1 question
        buffer.putShort((short) 0x0); // 0 answers
        buffer.putInt(0x0); // no nameservers or additional records
    }

    public void askOn(MulticastSocket socket) throws IOException {
        try {
            InetAddress group = InetAddress.getByName(ServiceQuery.MDNS_IP4_ADDRESS);
            DatagramPacket packet = new DatagramPacket(buffer.array(), buffer.position(), group, ServiceQuery.MDNS_PORT);
            packet.setAddress(group);
            socket.send(packet);
        } catch (UnknownHostException e) {
            System.err.println("UnknownHostException " + e);
        }
    }
}
