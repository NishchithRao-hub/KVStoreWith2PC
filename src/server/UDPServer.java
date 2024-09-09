package server;

import java.io.*;
import java.net.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class UDPServer {

  private static final int THREAD_POOL_SIZE = 10;
  private static final int THREAD_TIMEOUT_SECONDS = 60;

  public static void main(String[] args) {
    if (args.length < 2) {
      System.err.println("Usage: java UDPServer <port number> <rmi registry url> [<rmi registry url>...]");
      ServerLogger.logUDP("Usage: java UDPServer <port number> <rmi registry url> [<rmi registry url>...]");
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
      ServerLogger.logUDP("Connected to RMI KeyValueStore replicas");
    } catch (Exception e) {
      System.err.println("Failed to connect to RMI registry. Check server log for more details.");
      ServerLogger.logUDPError("Failed to connect to RMI registry", e);
      System.exit(1);
    }

    TwoPhaseCommitImpl twoPhaseCommit = new TwoPhaseCommitImpl(replicas);
    RequestProcessor.setTwoPhaseCommit(twoPhaseCommit);
    RequestProcessor.setReplicas(replicas);

    try (DatagramSocket serverSocket = new DatagramSocket(portNumber)) {
      System.out.println("UDP Server is running...");
      ServerLogger.logUDP("UDP Server is running...");

      byte[] receiveData = new byte[1024];

      while (true) {
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        serverSocket.receive(receivePacket);
        InetAddress clientAddress = receivePacket.getAddress();
        int clientPort = receivePacket.getPort();
        byte[] packetData = receivePacket.getData();
        int packetLength = receivePacket.getLength();

        threadPool.submitTask(() -> {
          try {
            String request = new String(packetData, 0, packetLength);
            System.out.println("Received from UDP client " + clientAddress + ":" + clientPort + ": " + request);
            ServerLogger.logUDP("Received from UDP client " + clientAddress + ":" + clientPort + ": " + request);

            // Process client request using RequestProcessor
            String response = RequestProcessor.processRequest(request);
            byte[] sendData = response.getBytes();

            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
            serverSocket.send(sendPacket);
            System.out.println("Sent to UDP client " + clientAddress + ":" + clientPort + ": " + response);
            ServerLogger.logUDP("Sent to UDP client " + clientAddress + ":" + clientPort + ": " + response);
          } catch (IOException e) {
            System.err.println("Error occurred while handling UDP client request. Check server log for more details.");
            ServerLogger.logUDPError("Error occurred while handling UDP client request.", e);
          }
        });
      }
    } catch (IOException e) {
      System.err.println("Error occurred while listening for UDP packets on port " + portNumber + ". Check server log for more details.");
      ServerLogger.logUDPError("Error occurred while listening for UDP packets on port " + portNumber, e);
    } finally {
      threadPool.shutdown();
    }
  }
}
