package server;

import java.io.*;
import java.net.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TCPServer {

  private static final int THREAD_POOL_SIZE = 10;
  private static final int THREAD_TIMEOUT_SECONDS = 30;

  public static void main(String[] args) {
    if (args.length < 2) {
      System.err.println("Usage: java TCPServer <port number> <rmi registry url> [<rmi registry url>...]");
      ServerLogger.logTCP("Usage: java TCPServer <port number> <rmi registry url> [<rmi registry url>...]");
      System.exit(1);
    }

    int portNumber = Integer.parseInt(args[0]);
    String[] registryURLs = java.util.Arrays.copyOfRange(args, 1, args.length);

    TimeoutThreadPool threadPool = new TimeoutThreadPool(THREAD_POOL_SIZE, THREAD_TIMEOUT_SECONDS);
    List<KeyValueStoreInterface> replicas = null;

    try {
      replicas = IntStream.range(0, registryURLs.length)
              .mapToObj(i -> {
                try {
                  String[] parts = registryURLs[i].split(":");
                  Registry registry = LocateRegistry.getRegistry(parts[0], Integer.parseInt(parts[1]));
                  return (KeyValueStoreInterface) registry.lookup("KeyValueStore" + (i + 1));
                } catch (Exception e) {
                  e.printStackTrace();
                  return null;
                }
              })
              .collect(Collectors.toList());

      System.out.println("Connected to RMI KeyValueStore replicas");
      ServerLogger.logTCP("Connected to RMI KeyValueStore replicas");
    } catch (Exception e) {
      System.err.println("Failed to connect to RMI registry. Check server log for more details.");
      ServerLogger.logTCPError("Failed to connect to RMI registry", e);
      System.exit(1);
    }

    TwoPhaseCommitImpl twoPhaseCommit = new TwoPhaseCommitImpl(replicas);
    RequestProcessor.setTwoPhaseCommit(twoPhaseCommit);
    RequestProcessor.setReplicas(replicas);

    try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
      System.out.println("TCP Server is running...");
      ServerLogger.logTCP("TCP Server is running...");

      while (true) {
        try {
          Socket clientSocket = serverSocket.accept();
          System.out.println("Connection established with " + clientSocket.getInetAddress());
          ServerLogger.logTCP("Connection established with " + clientSocket.getInetAddress());

          // Initialize RequestProcessor with TwoPhaseCommitImpl instance
          RequestProcessor.setTwoPhaseCommit(twoPhaseCommit);

          // Handle client request using a thread from the thread pool
          threadPool.submitTask(new ClientHandler(clientSocket));
        } catch (IOException e) {
          System.err.println("Error accepting client connection. Check server log for more details.");
          ServerLogger.logTCPError("Error accepting client connection.", e);
        }
      }
    } catch (IOException e) {
      System.err.println("Error occurred while listening for connections on port. Check server log for more details.");
      ServerLogger.logTCPError("Error occurred while listening for connections on port " + portNumber, e);
    } finally {
      threadPool.shutdown();
    }
  }

  static class ClientHandler implements Runnable {
    private Socket clientSocket;

    public ClientHandler(Socket clientSocket) {
      this.clientSocket = clientSocket;
    }

    @Override
    public void run() {

      try (
              PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
              BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      ) {
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
          System.out.println("Received from TCP client " + clientSocket.getInetAddress() + ": " + inputLine);
          ServerLogger.logTCP("Received from TCP client " + clientSocket.getInetAddress() + ": " + inputLine);

          // Process client request using RequestProcessor
          String response = RequestProcessor.processRequest(inputLine);

          System.out.println("Response to send: " + response);
          out.println(response);  // Send response to client
          System.out.println("Sent to TCP client " + clientSocket.getInetAddress() + ": " + response);
          ServerLogger.logTCP("Sent to TCP client " + clientSocket.getInetAddress() + ": " + response);
        }
      } catch (IOException e) {
        System.err.println("Error occurred while handling TCP client request. Check server log for more details.");
        ServerLogger.logTCPError("Error occurred while handling TCP client request.", e);
      } catch (Exception e) {
        ServerLogger.logTCPError("Error occurred while handling TCP client request.", e);
      } finally {
        try {
          clientSocket.close();
        } catch (IOException e) {
          System.err.println("Error closing client socket. Check server log for more details.");
          ServerLogger.logTCPError("Error closing client socket.", e);
        }
      }
    }
  }
}
