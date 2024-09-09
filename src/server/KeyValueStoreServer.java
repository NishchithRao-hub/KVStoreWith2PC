package server;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;


public class  KeyValueStoreServer {
  public static void main(String[] args) {

    try {
      // Initialize the list of replicas
      List<KeyValueStoreInterface> replicas = new ArrayList<>();
      for (int i = 0; i < 5; i++) {
        KeyValueStoreImpl replica = new KeyValueStoreImpl();
        replicas.add(replica);
        String replicaName = "KeyValueStore" + (i + 1);
        int port = 1099 + i; // Assign each replica a unique port
        Registry registry = LocateRegistry.createRegistry(port);
        Naming.rebind("//localhost:" + port + "/" + replicaName, replica);
        System.out.println(replicaName + " is running on port " + port);
      }

      // Create the primary server which will use TwoPhaseCommitImpl
      TwoPhaseCommitImpl twoPhaseCommit = new TwoPhaseCommitImpl(replicas);
      KeyValueStoreWith2PC primaryServer = new KeyValueStoreWith2PC(twoPhaseCommit);
      int primaryPort = 1104; // Use a different port for the primary server
      Registry primaryRegistry = LocateRegistry.createRegistry(primaryPort);
      Naming.rebind("//localhost:" + primaryPort + "/KeyValueStore", primaryServer);

      System.out.println("Primary KeyValueStoreServer with 2PC is running on port " + primaryPort);

    }

    catch (Exception e) {
      System.err.println("Server exception: " + e.toString());
      ServerLogger.logRMIServerError("Server exception: ", e);
    }
  }
}


