package TCP;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URL;

public class TCPClient {
    private Socket clientSocket;
    protected final String host;
    protected final int port;

    public TCPClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public TCPClient(URL url) {
        if (url == null) throw new RuntimeException("Invalid url");
        host = url.getHost();
        port = url.getPort();
    }

    public void start() {
        try {
            clientSocket = new Socket(host, port);
        } catch (IOException e) {
            System.out.println("Failed to connect to " + host + ": " + port);
            throw new RuntimeException(e);
        }
//        System.out.println("Connected to " + host + ": " + port);
    }

    public void stop() {
        try {
            if (isReady()) {
                clientSocket.close();
                System.out.println("Connection closed");
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
        if (!isReady()) start();
        clientSocket.getOutputStream().write(message);
        clientSocket.getOutputStream().flush();
    }

    public byte[] receiveMessage() throws IOException {
        if (!isReady()) start();
        InputStream is = clientSocket.getInputStream();
        StringBuilder sb = new StringBuilder();
        while (sb.isEmpty()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while (is.available() > 0 && (bytesRead = is.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, bytesRead));
            }
        }
        return sb.toString().getBytes();
    }
}
