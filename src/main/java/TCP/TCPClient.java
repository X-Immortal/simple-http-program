package TCP;

import utils.EncodingUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Arrays;

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
        if (isReady()) return;
        try {
            clientSocket = new Socket(host, port);
            clientSocket.setSoTimeout(100);
        } catch (IOException e) {
            System.out.println("Failed to connect to " + host + ": " + port);
        }
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
        if (clientSocket == null) return false;

        try {
            int timeout = clientSocket.getSoTimeout();
            clientSocket.setSoTimeout(10);
            try {
                if (clientSocket.getInputStream().read() == -1) {
                    clientSocket.close();
                }
            } catch (SocketTimeoutException ignored) {
            }
            clientSocket.setSoTimeout(timeout);
            return !clientSocket.isClosed();
        } catch (IOException e) {
            return false;
        }
    }

    public void sendMessage(byte[] message) throws IOException {
        if (!isReady()) throw new SocketException("Not connected");
        clientSocket.getOutputStream().write(message);
        clientSocket.getOutputStream().flush();
    }

    public byte[] receiveMessage() throws IOException {
        if (!isReady()) throw new SocketException("Not connected");
        InputStream is = clientSocket.getInputStream();
        StringBuilder sb = new StringBuilder();
        while (sb.length() == 0) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while (true) {
                try {
                    bytesRead = is.read(buffer);
                } catch (SocketTimeoutException e) {
                    break;
                }
                if (bytesRead == -1) {
                    clientSocket.close();
                    return null;
                }
                byte[] data;
                if (bytesRead == buffer.length) {
                    data = buffer;
                } else {
                    data = Arrays.copyOf(buffer, bytesRead);
                }
                sb.append(EncodingUtil.decodeBinary(data));
            }
        }
        return EncodingUtil.encodeBinary(sb.toString());
    }
}
