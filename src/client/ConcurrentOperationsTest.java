package client;

import server.KeyValueStoreInterface;
import server.TwoPhaseCommitImpl;
import java.rmi.Naming;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ConcurrentOperationsTest {

  private static final String[] SERVER_ADDRESSES = {
          "//localhost:1099/KeyValueStore1",
          "//localhost:1100/KeyValueStore2",
          "//localhost:1101/KeyValueStore3",
          "//localhost:1102/KeyValueStore4",
          "//localhost:1103/KeyValueStore5"
  };

  public static void main(String[] args) {
    int numConcurrentRequests = 10;  // Number of concurrent requests for each operation

    ExecutorService executorService = Executors.newFixedThreadPool(numConcurrentRequests * 3);

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

      // Perform PUT operations concurrently
      for (int i = 1; i <= numConcurrentRequests; i++) {
        final int index = i;
        executorService.submit(() -> {
          try {
            String key = "key" + index;
            String value = "value" + index;
            if (twoPhaseCommit.performTwoPhaseCommit("PUT", key, value)) {
              System.out.println("PUT operation succeeded for " + key);
            } else {
              System.out.println("PUT operation failed for " + key);
            }
          } catch (Exception e) {
            System.err.println("Client exception during PUT operation: " + e.toString());
            ClientLogger.logRMIClientError("Client exception during PUT operation: ", e);
          }
        });
      }

      // Perform GET operations concurrently
      for (int i = 1; i <= numConcurrentRequests; i++) {
        final int index = i;
        executorService.submit(() -> {
          try {
            String key = "key" + index;
            String response = replicas.get(0).get(key);  // Choose any replica for GET operations
            System.out.println("GET operation response for " + key + ": " + response);
          } catch (Exception e) {
            System.err.println("Client exception during GET operation: " + e.toString());
            ClientLogger.logRMIClientError("Client exception during GET operation: ", e);
          }
        });
      }

      // Perform DELETE operations concurrently
      for (int i = 1; i <= numConcurrentRequests; i++) {
        final int index = i;
        executorService.submit(() -> {
          try {
            String key = "key" + index;
            if (twoPhaseCommit.performTwoPhaseCommit("DELETE", key, "")) {
              System.out.println("DELETE operation succeeded for " + key);
            } else {
              System.out.println("DELETE operation failed for " + key);
            }
          } catch (Exception e) {
            System.err.println("Client exception during DELETE operation: " + e.toString());
            ClientLogger.logRMIClientError("Client exception during DELETE operation: ", e);
          }
        });
      }

      executorService.shutdown();
      while (!executorService.isTerminated()) {
        Thread.sleep(1000);
      }

      System.out.println(".....Concurrency Test Completed.....");
      ClientLogger.logRMIClient(".....Concurrency Test Completed.....");

    } catch (Exception e) {
      System.err.println("Client exception: " + e.toString());
      ClientLogger.logRMIClientError("Client exception: ", e);
    }
  }
}
