package server;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;


public class ReplicaKeyValueStoreServer {
  public static void main(String[] args) {
    // Define ports for each server instance
    int[] ports = {1099, 1100, 1101, 1102, 1103};

    for (int i = 0; i < ports.length; i++) {
      startServer(ports[i], "KeyValueStore" + (i + 1));
    }
  }

  private static void startServer(int port, String serverName) {
    try {
      KeyValueStoreImpl keyValueStore = new KeyValueStoreImpl();
      LocateRegistry.createRegistry(port);
      Naming.rebind("//localhost:" + port + "/" + serverName, keyValueStore);
      System.out.println(serverName + " is running on port " + port);
    } catch (Exception e) {
      System.err.println(serverName + " exception: " + e.toString());
      ServerLogger.logRMIServerError(serverName + " exception: ", e);
    }
  }
}
