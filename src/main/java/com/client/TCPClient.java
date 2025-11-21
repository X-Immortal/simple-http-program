package com.client;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.Socket;

public class TCPClient {
    private Socket clientSocket ;

    public TCPClient() {
    }

    public void start() {
        try {
            clientSocket = new Socket("localhost", 8080);
        } catch (IOException e) {
            System.out.println("Failed to connect to localhost");
            throw new RuntimeException(e);
        }
        System.out.println("Connect to localhost");
    }

    public void stop() {
        try {
            if (isReady()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            System.out.println("Failed to close tcp connect");
            throw new RuntimeException(e);
        }
    }

    public boolean isReady() {
        return clientSocket != null && !clientSocket.isClosed();
    }

    public void sendMessage(byte[] message) throws IOException {
        if (!isReady()) throw new IOException("TCPClient is not ready");
        clientSocket.getOutputStream().write(message);
        clientSocket.getOutputStream().flush();
    }

    public byte[] receiveMessage() throws IOException {
        if (!isReady()) throw new IOException("TCPClient is not ready");
        StringBuilder sb = new StringBuilder();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = clientSocket.getInputStream().read(buffer)) > 0) {
            sb.append(new String(buffer, 0, bytesRead));
        }
        return sb.toString().getBytes();
    }

    public void run() {
        while (isReady()) {

        }
    }

    public static void main(String[] args) {

    }
}
