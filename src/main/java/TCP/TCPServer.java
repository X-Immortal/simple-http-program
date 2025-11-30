package TCP;

import HTTP.utils.EncodingUtil;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
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

        public TCPClientHandler(Socket socket, Function<byte[], byte[]> handler) {
            this.clientSocket = socket;
            this.handler = handler;
        }

        @Override
        public void run() {
            try {
                while (isReady()) {
                    receiveMessage();
                    if (receivedMessage == null || receivedMessage.length == 0) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        continue;
                    }
                    sentMessage = handler.apply(receivedMessage);
                    sendMessage();
                }
            } catch (IOException e) {
                e.printStackTrace();
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
            while (is.available() > 0 && (bytesRead = is.read(buffer)) != -1) {
                byte[] data;
                if (bytesRead == buffer.length) {
                    data = buffer;
                } else {
                    data = Arrays.copyOf(buffer, bytesRead);
                }
                sb.append(EncodingUtil.decodeBinary(data));
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            receivedMessage = EncodingUtil.encodeBinary(sb.toString());
        }

        protected void sendMessage() throws IOException {
            if (!isReady()) {
                throw new RuntimeException("Client socket is not ready");
            }
            if (sentMessage == null) return;
            OutputStream os = clientSocket.getOutputStream();
            os.write(sentMessage);
            os.flush();
            showSentMessage.accept(sentMessage);
        }

        protected boolean isReady() {
            return clientSocket != null && !clientSocket.isClosed();
        }
    }
}
