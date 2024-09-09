package client;

import server.KeyValueStoreInterface;
import server.TwoPhaseCommitImpl;

import java.io.*;
import java.net.*;
import java.rmi.Naming;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TCPClient {

  public static void main(String[] args) {
    if (args.length < 5) {
      System.out.println("Usage: java TCPClient <hostname> <port> <operation> <key> [<value>]");
      ClientLogger.logTCP("Usage: java TCPClient <hostname> <port> <operation> <key> [<value>]");
      return;
    }

    String hostname = args[0];
    int port = Integer.parseInt(args[1]);
    String operation = args[2].toUpperCase();
    String key = args[3];
    String value = args.length > 4 ? args[4] : "";

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
                  ClientLogger.logTCPError("Failed to connect to RMI registry at " + serverAddresses[i], e);
                  return null;
                }
              })
              .filter(Objects::nonNull)
              .collect(Collectors.toList());

      if (replicas.isEmpty()) {
        System.err.println("Unable to connect to any RMI registry. Exiting...");
        System.exit(1);
      }

      TwoPhaseCommitImpl twoPhaseCommit = new TwoPhaseCommitImpl(replicas);

      try (Socket socket = new Socket()) {
        socket.connect(new InetSocketAddress(hostname, port), 5000); // 5 seconds timeout
        socket.setSoTimeout(5000); // 5 seconds read timeout

        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

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

        // Send the request to the server
        System.out.println("Sending request to server: " + request);
        out.println(request);
        String serverResponse = in.readLine();
        System.out.println("Response from server: " + serverResponse);
        ClientLogger.logTCP("Response from server: " + serverResponse);

        if (serverResponse == null) {
          System.err.println("No response received from the server.");
          ClientLogger.logTCP("No response received from the server.");
        }

        System.out.println("Final response: " + response);
        ClientLogger.logTCP("Final response: " + response);

      } catch (SocketTimeoutException e) {
        System.err.println("Socket timed out while connecting or reading from server.");
        ClientLogger.logTCPError("Socket timed out while connecting or reading from server.", e);
      } catch (IOException e) {
        ClientLogger.logTCPError("Check log for more details about the error", e);
      }

    } catch (Exception e) {
      System.err.println("Client exception: " + e.toString());
      ClientLogger.logTCPError("Client exception: ", e);
    }
  }
}
