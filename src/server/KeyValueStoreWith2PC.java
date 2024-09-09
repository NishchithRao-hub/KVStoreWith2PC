package server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class KeyValueStoreWith2PC extends UnicastRemoteObject implements KeyValueStoreInterface {
  private TwoPhaseCommitImpl twoPhaseCommit;

  public KeyValueStoreWith2PC(TwoPhaseCommitImpl twoPhaseCommit) throws RemoteException {
    this.twoPhaseCommit = twoPhaseCommit;
  }

  @Override
  public String put(String key, String value) throws RemoteException {
    boolean success = twoPhaseCommit.performTwoPhaseCommit("PUT", key, value);
    return success ? "PUT OK" : "PUT ERROR";
  }

  @Override
  public String delete(String key) throws RemoteException {
    boolean success = twoPhaseCommit.performTwoPhaseCommit("DELETE", key, null);
    return success ? "DELETE OK" : "DELETE ERROR";
  }

  @Override
  public String get(String key) throws RemoteException {
    // Any replica can handle GET requests, so we can delegate to one of the replicas.
    return twoPhaseCommit.getReplicas().get(0).get(key);
  }

  @Override
  public boolean prepare(String operation, String key, String value) throws RemoteException {
    throw new UnsupportedOperationException("Prepare not supported on primary server");
  }

  @Override
  public void commit(String operation, String key, String value) throws RemoteException {
    throw new UnsupportedOperationException("Commit not supported on primary server");
  }

  @Override
  public void abort(String operation, String key, String value) throws RemoteException {
    throw new UnsupportedOperationException("Abort not supported on primary server");
  }
}
