package client;

import server.KeyValueStoreInterface;
import server.TwoPhaseCommitImpl;

import java.io.*;
import java.net.*;
import java.rmi.Naming;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class UDPClient {
  public static void main(String[] args) {
    if (args.length < 4) {
      System.out.println("Usage: java UDPClient <hostname> <port> <operation> <key> [<value>]");
      ClientLogger.logUDP("Usage: java UDPClient <hostname> <port> <operation> <key> [<value>]");
      return;
    }

    String hostname = args[0];
    int port = Integer.parseInt(args[1]);
    String operation = args[2].toUpperCase();
    String key = args[3];
    String value = args.length == 5 ? args[4] : null;

    String[] serverAddresses = {
            "//localhost:1099/KeyValueStore1",
            "//localhost:1100/KeyValueStore2",
            "//localhost:1101/KeyValueStore3",
            "//localhost:1102/KeyValueStore4",
            "//localhost:1103/KeyValueStore5"
    };

    try {
      List<KeyValueStoreInterface> replicas = IntStream.range(0, serverAddresses.length)
              .mapToObj(i -> {
                try {
                  return (KeyValueStoreInterface) Naming.lookup(serverAddresses[i]);
                } catch (Exception e) {
                  System.err.println("Failed to connect to RMI registry at " + serverAddresses[i]);
                  ClientLogger.logUDPError("Failed to connect to RMI registry at " + serverAddresses[i], e);
                  return null;
                }
              })
              .filter(r -> r != null)
              .collect(Collectors.toList());

      if (replicas.isEmpty()) {
        System.err.println("Unable to connect to any RMI registry. Exiting...");
        System.exit(1);
      }

      TwoPhaseCommitImpl twoPhaseCommit = new TwoPhaseCommitImpl(replicas);

      try (DatagramSocket socket = new DatagramSocket()) {
        socket.setSoTimeout(5000); // 5 seconds timeout

        InetAddress address = InetAddress.getByName(hostname);
        String request;
        String response = "";

        switch (operation) {
          case "PUT":
            if (value == null) {
              System.err.println("PUT operation requires a value.");
              return;
            }
            if (twoPhaseCommit.performTwoPhaseCommit("PUT", key, value)) {
              response = "PUT operation succeeded";
            } else {
              response = "PUT operation failed";
            }
            request = "PUT " + key + " " + value;
            break;
          case "GET":
            // Handle GET request directly with a replica
            response = replicas.get(0).get(key);
            request = "GET " + key;
            break;
          case "DELETE":
            if (twoPhaseCommit.performTwoPhaseCommit("DELETE", key, value)) {
              response = "DELETE operation succeeded";
            } else {
              response = "DELETE operation failed";
            }
            request = "DELETE " + key;
            break;
          default:
            System.err.println("Invalid operation. Use PUT, GET, or DELETE.");
            return;
        }


        byte[] sendData = request.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
        socket.send(sendPacket);

        byte[] receiveData = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        socket.receive(receivePacket);

        String serverResponse = new String(receivePacket.getData(), 0, receivePacket.getLength());
        System.out.println("Response from server: " + serverResponse);
        ClientLogger.logUDP("Response from server: " + serverResponse);
        System.out.println("Final response: " + response);
        ClientLogger.logUDP("Final response: " + response);

      } catch (SocketTimeoutException e) {
        System.err.println("Socket timed out while waiting for server response.");
        ClientLogger.logUDPError("Socket timed out while waiting for server response.", e);
      } catch (IOException e) {
        ClientLogger.logUDPError("Check client log for more details about the error", e);
      }

    } catch (Exception e) {
      System.err.println("Client exception: " + e.toString());
      ClientLogger.logUDPError("Client exception: ", e);
    }
  }
}
