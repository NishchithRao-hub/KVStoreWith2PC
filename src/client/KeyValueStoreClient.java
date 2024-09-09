package client;

import server.KeyValueStoreInterface;
import server.TwoPhaseCommitImpl;
import java.rmi.Naming;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class KeyValueStoreClient {

  public static void main(String[] args) {
    if (args.length != 4 && args.length != 5) {
      System.err.println("Usage: java KeyValueStoreClient <server> <operation> <key> [<value>] <num_concurrent_requests>");
      ClientLogger.logRMIClient("Usage: java KeyValueStoreClient <server> <operation> <key> [<value>] <num_concurrent_requests>");
      System.exit(1);
    }

    String operation = args[1].toUpperCase();
    String key = args[2];
    String value = args.length == 4 ? null : args[3];
    int numConcurrentRequests = Integer.parseInt(args[4]);

    String[] serverAddresses = {
            "//localhost:1099/KeyValueStore1",
            "//localhost:1100/KeyValueStore2",
            "//localhost:1101/KeyValueStore3",
            "//localhost:1102/KeyValueStore4",
            "//localhost:1103/KeyValueStore5"
    };

    prePopulateStore(serverAddresses);

    ExecutorService executorService = Executors.newFixedThreadPool(numConcurrentRequests);

    try {
      List<KeyValueStoreInterface> replicas = IntStream.range(0, serverAddresses.length)
              .mapToObj(i -> {
                try {
                  return (KeyValueStoreInterface) Naming.lookup(serverAddresses[i]);
                } catch (Exception e) {
                  System.err.println("Failed to connect to RMI registry at " + serverAddresses[i]);
                  ClientLogger.logRMIClientError("Failed to connect to RMI registry at " + serverAddresses[i], e);
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

      for (int i = 0; i < numConcurrentRequests; i++) {
        KeyValueStoreInterface finalKeyValueStore = replicas.get(0); // Choose any replica for GET operations
        executorService.submit(() -> {
          try {
            String response = "";
            switch (operation) {
              case "PUT":
                if (value == null) {
                  System.err.println("PUT operation requires a value.");
                  System.exit(1);
                }
                if (twoPhaseCommit.performTwoPhaseCommit("PUT", key, value)) {
                  response = "PUT operation succeeded";
                } else {
                  response = "PUT operation failed";
                }
                break;
              case "GET":
                response = finalKeyValueStore.get(key);
                break;
              case "DELETE":
                if (twoPhaseCommit.performTwoPhaseCommit("DELETE", key, value)) {
                  response = "DELETE operation succeeded";
                } else {
                  response = "DELETE operation failed";
                }
                break;
              default:
                System.err.println("Invalid operation. Use PUT, GET, or DELETE.");
                System.exit(1);
            }
            System.out.println("Response from server: " + response);
            ClientLogger.logRMIClient("Response from server: " + response);
          } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            ClientLogger.logRMIClientError("Client exception: ", e);
          }
        });
      }

      executorService.shutdown();

    } catch (Exception e) {
      System.err.println("Client exception: " + e.toString());
      ClientLogger.logRMIClientError("Client exception: ", e);
    }
  }

  private static void prePopulateStore(String[] serverAddresses) {
    try {
      List<KeyValueStoreInterface> replicas = IntStream.range(0, serverAddresses.length)
              .mapToObj(i -> {
                try {
                  String[] parts = serverAddresses[i].split(":");
                  Registry registry = LocateRegistry.getRegistry(parts[0].replace("//", ""), Integer.parseInt(parts[1].split("/")[0]));
                  return (KeyValueStoreInterface) registry.lookup(parts[1].split("/")[1]);
                } catch (Exception e) {
                  e.printStackTrace();
                  return null;
                }
              })
              .collect(Collectors.toList());

      TwoPhaseCommitImpl twoPhaseCommit = new TwoPhaseCommitImpl(replicas);

      // Pre-populate with some data
      for (int i = 1; i <= 5; i++) {
        String key = "key" + i;
        String value = "value" + i;
        boolean success = twoPhaseCommit.performTwoPhaseCommit("PUT", key, value);

        if (success) {
          System.out.println("Pre-populated " + key + " with " + value);
          ClientLogger.logRMIClient("Pre-populated " + key + " with " + value);
        } else {
          System.err.println("Failed to pre-populate " + key + " with " + value);
          ClientLogger.logRMIClientError("Failed to pre-populate " + key + " with " + value, null);
        }
      }
    } catch (Exception e) {
      System.err.println("Exception during pre-population: " + e.toString());
      ClientLogger.logRMIClientError("Exception during pre-population: ", e);
    }
  }

}
