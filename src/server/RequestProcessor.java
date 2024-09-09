package server;

import java.rmi.RemoteException;
import java.util.List;

public class RequestProcessor {

  private static TwoPhaseCommitImpl twoPhaseCommit;
  private static List<KeyValueStoreInterface> replicas;

  public static void setTwoPhaseCommit(TwoPhaseCommitImpl twoPhaseCommit) {
    RequestProcessor.twoPhaseCommit = twoPhaseCommit;
  }

  public static void setReplicas(List<KeyValueStoreInterface> replicas) {
    RequestProcessor.replicas = replicas;
  }

  public static TwoPhaseCommitImpl getTwoPhaseCommit() {
    return twoPhaseCommit;
  }

  public static String processRequest(String request) {
    String[] parts = request.split(" ");
    if (parts.length < 2) {
      return "ERROR Malformed request: " + request;
    }

    String command = parts[0];
    String key = parts[1];
    String value = "";

    try {
      switch (command) {
        case "PUT":
          if (parts.length != 3) {
            return "ERROR Malformed PUT request: " + request;
          }
          value = parts[2];
          boolean result = twoPhaseCommit.performTwoPhaseCommit("PUT", key, value);
          System.out.println("PUT result: " + result);
          return result ? "PUT OK" : "PUT ERROR";

        case "GET":
          String getValue = replicas.get(0).get(key);
          System.out.println("GET result: " + getValue);
          return getValue;

        case "DELETE":
          boolean deleteResult = twoPhaseCommit.performTwoPhaseCommit("DELETE", key, null);
          System.out.println("DELETE result: " + deleteResult);
          return deleteResult ? "DELETE OK" : "DELETE ERROR";

        default:
          return "ERROR Invalid command: " + command;
      }
    } catch (RemoteException e) {
      System.out.println("ERROR Remote exception: " + e.getMessage());
      return "ERROR Remote exception: " + e.getMessage();
    }
  }
}
