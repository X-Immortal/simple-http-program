package TCP;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class TCPServer {
    private ServerSocket serverSocket;
    private final int port;
    private final ExecutorService threadPool = Executors.newFixedThreadPool(10);

    public TCPServer(int port) {
        this.port = port;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to start server");
        }
        System.out.println("Server started");
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

    public boolean isReady() {
        return serverSocket != null && !serverSocket.isClosed();
    }

    public void run(Consumer<byte[]> handler) {
        while (isReady()) {
            try {
                Socket clientSocket = serverSocket.accept();
                threadPool.execute(new ClientHandler(clientSocket, handler));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    class ClientHandler implements Runnable {
        private Socket clientSocket;
        private byte[] message;
        Consumer<byte[]> handler;

        public ClientHandler(Socket socket, Consumer<byte[]> handler) {
            this.clientSocket = socket;
            this.handler = handler;
        }

        @Override
        public void run() {
            try {
                while (clientSocket != null && !clientSocket.isClosed()) {
                    receiveMessage();
                    handler.accept(message);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void receiveMessage() throws IOException {
            // TODO: 实现TCP消息接收
            if (clientSocket == null || clientSocket.isClosed()) {
                throw new IOException("Client socket is not ready");
            }
            InputStream is = clientSocket.getInputStream();
            StringBuilder sb = new StringBuilder();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while (is.available() > 0 && (bytesRead = is.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, bytesRead));
            }
            message = sb.toString().getBytes();
        }
    }

    // 测试用，应先启动
    public static void main(String[] args) {
        TCPServer server = new TCPServer(8080);
        server.start();
        if (!server.isReady())  throw new RuntimeException("Failed to start server");
        Thread serverThread = new Thread(() -> server.run(bytes -> {
            if (bytes == null || bytes.length == 0) return;
            System.out.println("Received message: ");
            System.out.println(new String(bytes));
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
