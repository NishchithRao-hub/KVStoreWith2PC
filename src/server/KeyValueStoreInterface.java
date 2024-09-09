package server;

import java.rmi.Remote;
import java.rmi.RemoteException;

// This interface defines the remote methods that the Key-Value Store Server provides. Throws
// exception if a remote communication error occurs.
public interface KeyValueStoreInterface extends Remote {
  String put(String key, String value) throws RemoteException;
  String get(String key) throws RemoteException;
  String delete(String key) throws RemoteException;
  boolean prepare(String operation, String key, String value) throws RemoteException;
  void commit(String operation, String key, String value) throws RemoteException;
  void abort(String operation, String key, String value) throws RemoteException;

}
