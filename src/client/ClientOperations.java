package client;

import server.KeyValueStoreInterface;
import server.TwoPhaseCommitImpl;

import java.rmi.Naming;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ClientOperations {

  private static final String[] SERVER_ADDRESSES = {
          "//localhost:1099/KeyValueStore1",
          "//localhost:1100/KeyValueStore2",
          "//localhost:1101/KeyValueStore3",
          "//localhost:1102/KeyValueStore4",
          "//localhost:1103/KeyValueStore5"
  };

  public static void main(String[] args) {
    int numConcurrentRequests = 1; // Can be adjusted if needed
    performOperations(numConcurrentRequests);
  }

  public static void performOperations(int numConcurrentRequests) {
    String[] keys = {"key1", "key2", "key3", "key4", "key5"};
    String[] putValues = {"value1", "value2", "value3", "value4", "value5"};

    ExecutorService executorService = Executors.newFixedThreadPool(numConcurrentRequests);

    try {
      List<KeyValueStoreInterface> replicas = IntStream.range(0, SERVER_ADDRESSES.length)
              .mapToObj(i -> {
                try {
                  return (KeyValueStoreInterface) Naming.lookup(SERVER_ADDRESSES[i]);
                } catch (Exception e) {
                  System.err.println("Failed to connect to RMI registry at " + SERVER_ADDRESSES[i]);
                  ClientLogger.logRMIClientError("Failed to connect to RMI registry at " + SERVER_ADDRESSES[i], e);
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

      // Perform 5 PUT operations
      for (int i = 0; i < 5; i++) {
        final int index = i;
        executorService.submit(() -> {
          try {
            String key = keys[index];
            String value = putValues[index];
            if (twoPhaseCommit.performTwoPhaseCommit("PUT", key, value)) {
              System.out.println("PUT operation succeeded for " + key);
            } else {
              System.out.println("PUT operation failed for " + key);
            }
          } catch (Exception e) {
            System.err.println("Client exception during PUT: " + e.toString());
            ClientLogger.logRMIClientError("Client exception during PUT: ", e);
          }
        });
      }

      // Perform 5 GET operations
      for (int i = 0; i < 5; i++) {
        final int index = i;
        executorService.submit(() -> {
          try {
            String key = keys[index];
            KeyValueStoreInterface replica = replicas.get(index % replicas.size());
            String response = replica.get(key);
            System.out.println("GET operation response for " + key + ": " + response);
          } catch (Exception e) {
            System.err.println("Client exception during GET: " + e.toString());
            ClientLogger.logRMIClientError("Client exception during GET: ", e);
          }
        });
      }

      // Perform 5 DELETE operations
      for (int i = 0; i < 5; i++) {
        final int index = i;
        executorService.submit(() -> {
          try {
            String key = keys[index];
            if (twoPhaseCommit.performTwoPhaseCommit("DELETE", key, null)) {
              System.out.println("DELETE operation succeeded for " + key);
            } else {
              System.out.println("DELETE operation failed for " + key);
            }
          } catch (Exception e) {
            System.err.println("Client exception during DELETE: " + e.toString());
            ClientLogger.logRMIClientError("Client exception during DELETE: ", e);
          }
        });
      }

      executorService.shutdown();
      while (!executorService.isTerminated()) {
        // Wait for all tasks to finish
      }
      System.out.println("All operations are completed.");

    } catch (Exception e) {
      System.err.println("Client exception: " + e.toString());
      ClientLogger.logRMIClientError("Client exception: ", e);
    }
  }
}
