package TCP;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
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

    protected void start() {
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
            // TODO: 实现TCP消息接收
            if (!isReady()) {
                throw new IOException("Client socket is not ready");
            }
            InputStream is = clientSocket.getInputStream();
            StringBuilder sb = new StringBuilder();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while (is.available() > 0 && (bytesRead = is.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, bytesRead));
            }
            receivedMessage = sb.toString().getBytes();
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



    // 测试用，应先启动
    public static void main(String[] args) {
        TCPServer server = new TCPServer(8080);
        server.start();
        if (!server.isReady())  throw new RuntimeException("Failed to start server");
        Thread serverThread = new Thread(() -> server.run(bytes -> {
            if (bytes == null || bytes.length == 0) return null;
            System.out.println("Received message: ");
            System.out.println(new String(bytes));
            return ("Server received message: " + new String(bytes)).getBytes();
        }));
        Thread cmdThread = new Thread(() -> {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                try {
                    String input = br.readLine();
                    if ("exit".equals(input)) {
                        serverThread.stop();
                        server.stop();
                        System.exit(0);
                    }
                } catch (IOException e) {
                    server.stop();
                    throw new RuntimeException(e);
                }
            }
        });
        serverThread.start();
        cmdThread.start();
    }
}
