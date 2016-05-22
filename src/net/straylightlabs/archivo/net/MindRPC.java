/*
 * Copyright 2015-2016 Todd Kulesza <todd@dropline.net>.
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

package net.straylightlabs.archivo.net;

import net.straylightlabs.archivo.Archivo;
import org.json.JSONObject;

import javax.net.ssl.*;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MindRPC implements the MRPC/2 protocol for communicating with TiVo Minds.
 */

public class MindRPC {
    private final InetAddress address;
    private final int port;
    private final String mak;
    private final SSLSocketFactory socketFactory;
    private SSLSocket socket;
    private PrintWriter socketWriter;
    private BufferedInputStream socketReader;
    private final int sessionId;
    private int requestId;

    public static final int SCHEMA_VER = 9;
    public static final String LINE_ENDING = "\r\n";
    private static final String KEY_PATH = "resources/cdata.p12";
    private static final Pattern RESPONSE_HEAD = Pattern.compile("MRPC/2\\s+(\\d+)\\s+(\\d+)");
    private static final String KEY_PASSWORD = "LwrbLEFYvG";
    private static final int MAX_SESSION_ID_VAL = 0x27dc20;
    private static final String ENCODING = "UTF-8";

    public MindRPC(InetAddress address, int port, String mak) {
        this.address = address;
        this.port = port;
        this.mak = mak;
        this.requestId = 1;
        this.sessionId = new Random().nextInt(MAX_SESSION_ID_VAL);
        socketFactory = createSecureSocketFactory();
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getSessionId() {
        return sessionId;
    }

    public int nextRequestId() {
        return requestId++;
    }

    private synchronized void connectAndAuthenticate() throws IOException {
        if (socket == null || socket.isClosed()) {
            connect();
            authenticate();
        }
    }

    private void connect() throws IOException {
        socket = (SSLSocket) socketFactory.createSocket(address, port);
        socket.setNeedClientAuth(true);
        socket.setEnableSessionCreation(true);
        socket.startHandshake();
        socketWriter = new PrintWriter(socket.getOutputStream());
        socketReader = new BufferedInputStream(socket.getInputStream());
    }

    private void authenticate() throws IOException {
        MindCommandAuth authCommand = new MindCommandAuth(mak);
        authCommand.executeOn(this);
        if (authCommand.credentialsRejected()) {
            throw new MindCommandAuthException();
        }
    }

    private SSLSocketFactory createSecureSocketFactory() {
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            KeyStore store = createKeyStore();
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(store, KEY_PASSWORD.toCharArray());
            TrustManager[] trustManagers = new TrustManager[]{new AllTrustingTrustManager()};
            context.init(keyManagerFactory.getKeyManagers(), trustManagers, null);
            return context.getSocketFactory();
        } catch (GeneralSecurityException e) {
            Archivo.logger.error("Error creating custom SSLSocketFactory: ", e);
        }
        throw new AssertionError();
    }

    private KeyStore createKeyStore() throws KeyStoreException, NoSuchAlgorithmException, CertificateException {
        KeyStore store = KeyStore.getInstance("PKCS12");
        try (InputStream key = Archivo.class.getClassLoader().getResourceAsStream(KEY_PATH)) {
            assert (key != null);
            store.load(key, KEY_PASSWORD.toCharArray());
        } catch (IOException e) {
            Archivo.logger.error("Error accessing key file: ", e);
        }
        return store;
    }

    public JSONObject send(String request) throws IOException {
        Archivo.logger.info("Request to send:{}\n ", request);

        connectAndAuthenticate();
        socketWriter.print(request);
        socketWriter.flush();
        // The first line tells us how long the response will be
        String headerStart = readLine(socketReader);
        Matcher matcher = RESPONSE_HEAD.matcher(headerStart);
        if (matcher.find()) {
            int headerLength = Integer.parseInt(matcher.group(1));
            int bodyLength = Integer.parseInt(matcher.group(2));
            byte[] header = new byte[headerLength];
            byte[] body = new byte[bodyLength];
            if (!readBytes(socketReader, header, headerLength)) {
                throw new IOException("Error reading RPC response header");
            }
            if (!readBytes(socketReader, body, bodyLength)) {
                throw new IOException("Error reading RPC response body");
            }
            return new JSONObject(new String(body, ENCODING));
        } else {
            throw new IOException("Response format not as expected (First line = '" + headerStart + "'");
        }
    }

    /**
     * Read a \n-delimited line from an input stream.
     *
     * @param reader The BufferedInputStream to read from.
     * @return A String representing the next line from @reader.
     * @throws IOException
     */
    private String readLine(BufferedInputStream reader) throws IOException {
        byte[] bytes = new byte[128];
        int index = 0;
        while (true) {
            int val = reader.read();
            if (val == -1 || val == '\n') {
                break;
            } else {
                if (index == bytes.length) {
                    byte[] tmp = bytes;
                    bytes = new byte[tmp.length * 2];
                    System.arraycopy(tmp, 0, bytes, 0, tmp.length);
                }
                bytes[index++] = (byte) val;
            }
        }
        return new String(bytes, ENCODING);
    }

    private boolean readBytes(BufferedInputStream socketReader, byte[] buffer, int expectedLen) throws IOException {
        int totalBytesRead = 0;
        for (int bytesRead = 0; bytesRead != -1 && totalBytesRead < expectedLen; ) {
            bytesRead = socketReader.read(buffer, totalBytesRead, expectedLen - totalBytesRead);
            if (bytesRead > 0) {
                totalBytesRead += bytesRead;
            }
        }
        return totalBytesRead == expectedLen;
    }

    /**
     * Accept self-signed certificates, because that's what TiVo uses.
     */
    private static class AllTrustingTrustManager extends X509ExtendedTrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s, Socket socket) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s, Socket socket) throws CertificateException {
        }

        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s, SSLEngine sslEngine) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s, SSLEngine sslEngine) throws CertificateException {
        }

        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
