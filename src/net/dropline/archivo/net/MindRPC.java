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

package net.dropline.archivo.net;

import org.json.JSONObject;

import javax.net.ssl.*;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private BufferedReader socketReader;
    private final int sessionId;
    private int requestId;

    private static final Pattern RESPONSE_HEAD;

    public static final int SCHEMA_VER = 9;
    public static final String LINE_ENDING = "\r\n";
    private static final String KEY_PASSWORD = "LwrbLEFYvG";
    private static final int DEFAULT_PORT = 1413;
    private static final int MAX_SESSION_ID_VAL = 0x27dc20;

    static {
        RESPONSE_HEAD = Pattern.compile("MRPC/2\\s+(\\d+)\\s+(\\d+)");
    }

    public MindRPC(InetAddress address, String mak) {
        this(address, DEFAULT_PORT, mak);
    }

    public MindRPC(InetAddress address, int port, String mak) {
        this.address = address;
        this.port = port;
        this.mak = mak;
        this.requestId = 1;
        this.sessionId = new Random().nextInt(MAX_SESSION_ID_VAL);
        socketFactory = createSecureSocketFactory();
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
        socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    private void authenticate() throws IOException {
        MindCommand authCommand = new MindCommandAuth(mak);
        authCommand.executeOn(this);
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
            System.err.println("Error creating custom SSLSocketFactory: " + e.getLocalizedMessage());
        }
        throw new AssertionError();
    }

    private KeyStore createKeyStore() throws KeyStoreException, NoSuchAlgorithmException, CertificateException {
        KeyStore store = KeyStore.getInstance("PKCS12");
        Path keyPath = Paths.get(System.getProperty("user.dir"), "resources", "cdata.p12");
        try (InputStream key = Files.newInputStream(keyPath)) {
            store.load(key, KEY_PASSWORD.toCharArray());
        } catch (IOException e) {
            System.err.println("Error accessing key file: " + e.getLocalizedMessage());
        }
        return store;
    }

    public JSONObject send(String request) throws IOException {
        System.out.print("Request to send:\n" + request);

        connectAndAuthenticate();
        socketWriter.print(request);
        socketWriter.flush();
        String headerStart = socketReader.readLine();
        Matcher matcher = RESPONSE_HEAD.matcher(headerStart);
        if (matcher.find()) {
            int headerLength = Integer.parseInt(matcher.group(1));
            int bodyLength = Integer.parseInt(matcher.group(2));
            char[] header = new char[headerLength];
            char[] body = new char[bodyLength];
            socketReader.read(header, 0, headerLength);
            socketReader.read(body, 0, bodyLength);
            return new JSONObject(new String(body));
        } else {
            throw new IOException("Response format not as expected (First line = '" + headerStart + "'");
        }
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
