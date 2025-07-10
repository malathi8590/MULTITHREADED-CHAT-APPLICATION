import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

// Multithreaded Chat Application in one file
public class ChatApp {
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Type 'server' to start server or 'client' to start client:");
        String choice = scanner.nextLine();

        if (choice.equalsIgnoreCase("server")) {
            new Server().startServer();
        } else if (choice.equalsIgnoreCase("client")) {
            new Client().startClient();
        } else {
            System.out.println("Invalid option.");
        }
    }
}

// Server class
class Server {
    private final int port = 12345;
    private final Set<ClientHandler> clientHandlers = ConcurrentHashMap.newKeySet();

    public void startServer() {
        System.out.println("Server starting on port " + port + "...");
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected: " + socket.getInetAddress());
                ClientHandler handler = new ClientHandler(socket, this);
                clientHandlers.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clientHandlers) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    public void removeClient(ClientHandler handler) {
        clientHandlers.remove(handler);
        System.out.println("Client disconnected.");
    }

    // Inner ClientHandler class
    static class ClientHandler implements Runnable {
        private final Socket socket;
        private final Server server;
        private PrintWriter out;

        public ClientHandler(Socket socket, Server server) {
            this.socket = socket;
            this.server = server;
        }

        public void run() {
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            ) {
                out = new PrintWriter(socket.getOutputStream(), true);
                out.println("Welcome to the chat!");

                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("Received: " + message);
                    server.broadcast(message, this);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                server.removeClient(this);
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void sendMessage(String message) {
            if (out != null) {
                out.println(message);
            }
        }
    }
}

// Client class
class Client {
    private final String host = "localhost";
    private final int port = 12345;

    public void startClient() {
        try (
            Socket socket = new Socket(host, port);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Scanner scanner = new Scanner(System.in);
        ) {
            System.out.println("Connected to server at " + host + ":" + port);

            // Read messages from server in a separate thread
            Thread readerThread = new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                        System.out.println(serverMessage);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            readerThread.start();

            // Send user input to server
            while (true) {
                String userMessage = scanner.nextLine();
                out.println(userMessage);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
