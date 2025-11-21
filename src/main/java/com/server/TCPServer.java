package com.server;

import com.client.TCPClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServer {
    private ServerSocket serverSocket;
    private final ExecutorService threadPool = Executors.newFixedThreadPool(10);

    public TCPServer() {
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(8080);
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
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to stop server");
        }
    }

    public boolean isReady() {
        return serverSocket != null && !serverSocket.isClosed();
    }

    public void run() {
        while (isReady()) {
            try {
                Socket clientSocket = serverSocket.accept();
                threadPool.execute(new ClientHandler(clientSocket));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    class ClientHandler implements Runnable {
        private Socket clientSocket;
        private byte[] message;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                receiveMessage();
                handleRequest();
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
            while ((bytesRead = is.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, bytesRead));
            }
            message = sb.toString().getBytes();
        }

        private void handleRequest() throws IOException {
            // TODO: 实现TCP请求处理
            System.out.print("Received message: ");
            System.out.println(new String(message));
        }
    }

    public static void main(String[] args) {
        TCPServer server = new TCPServer();
        server.start();
        if (!server.isReady())  throw new RuntimeException("Failed to start server");
        new Thread(server::run).start();
        new Thread(() -> {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                try {
                    String input = br.readLine();
                    if ("exit".equals(input)) {
                        server.stop();
                        System.exit(0);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }
}
