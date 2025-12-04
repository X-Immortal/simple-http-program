package TCP;

import utils.EncodingUtil;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

public class TCPServer {
    private ServerSocket serverSocket;
    private final int port;
    protected final ExecutorService threadPool = Executors.newFixedThreadPool(10);

    protected Consumer<byte[]> showReceivedMessage = arg -> {};
    protected Consumer<byte[]> showSentMessage = arg -> {};
    protected Consumer<Integer> showStartInfo = arg -> {};

    public TCPServer(int port) {
        this.port = port;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        showStartInfo.accept(port);
    }

    public void stop() {
        try {
            if (isReady()) {
                serverSocket.close();
            }
            threadPool.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to stop server");
        }
    }

    public void setShowReceivedMessage(Consumer<byte[]> handler) {
        this.showReceivedMessage = handler;
    }

    public void setShowSentMessage(Consumer<byte[]> handler) {
        this.showSentMessage = handler;
    }

    public void setShowStartInfo(Consumer<Integer> handler) {
        this.showStartInfo = handler;
    }

    public boolean isReady() {
        return serverSocket != null && !serverSocket.isClosed();
    }

    public void run(Function<byte[], byte[]> handler) {
        if (!isReady()) start();
        while (isReady()) {
            try {
                Socket clientSocket = serverSocket.accept();
                threadPool.execute(new TCPClientHandler(clientSocket, handler));
            } catch (IOException e) {
                stop();
                return;
            }
        }
    }

    protected class TCPClientHandler implements Runnable {
        protected final Socket clientSocket;
        protected byte[] receivedMessage;
        protected byte[] sentMessage;
        protected Function<byte[], byte[]> handler;
        protected final int timeout = 5000;

        public TCPClientHandler(Socket socket, Function<byte[], byte[]> handler) {
            this.clientSocket = socket;
            this.handler = handler;
            try {
                clientSocket.setSoTimeout(500);
            } catch (SocketException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void run() {
            try {
                while (isReady()) {
                    receiveMessage();
                    if (receivedMessage == null || receivedMessage.length == 0) {
                        continue;
                    }
                    sentMessage = handler.apply(receivedMessage);
                    sendMessage();
                }
            } catch (IOException ignored) {
            }
        }

        protected void receiveMessage() throws IOException {
            if (!isReady()) {
                throw new IOException("Client socket is not ready");
            }
            InputStream is = clientSocket.getInputStream();
            StringBuilder sb = new StringBuilder();
            byte[] buffer = new byte[1024];
            int bytesRead;
            int totalTime = 0;
            while (true) {
                try {
                    bytesRead = is.read(buffer);
                    totalTime = 0;
                } catch (SocketTimeoutException e) {
                    totalTime += clientSocket.getSoTimeout();
                    if (sb.length() == 0 && totalTime < timeout) {
                        continue;
                    }
                    break;
                }
                if (bytesRead == -1) {
                    clientSocket.close();
                    receivedMessage = null;
                    return;
                }
                byte[] data;
                if (bytesRead == buffer.length) {
                    data = buffer;
                } else {
                    data = Arrays.copyOf(buffer, bytesRead);
                }
                sb.append(EncodingUtil.decodeBinary(data));
            }
            receivedMessage = EncodingUtil.encodeBinary(sb.toString());
        }

        protected void sendMessage() throws IOException {
            if (!isReady()) {
                throw new IOException("Client socket is not ready");
            }
            if (sentMessage == null) return;
            OutputStream os = clientSocket.getOutputStream();
            os.write(sentMessage);
            os.flush();
            showSentMessage.accept(sentMessage);
        }

        protected boolean isReady() {
            return clientSocket != null && !clientSocket.isClosed() && clientSocket.isConnected();
        }
    }
}
