package server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

// This class implements the KeyValueStoreInterface, providing the actual logic for the key-value
// store operations.
public class KeyValueStoreImpl extends UnicastRemoteObject implements KeyValueStoreInterface {
  // A thread-safe map to store key-value pairs.
  private ConcurrentHashMap<String, String> store;
  private ConcurrentHashMap<String, String> prepareStore;
  private String pendingOperation;
  private String pendingKey;
  private String pendingValue;

  public KeyValueStoreImpl() throws RemoteException {
    store = new ConcurrentHashMap<>();
    prepareStore = new ConcurrentHashMap<>();
  }

  @Override
  public String put(String key, String value) throws RemoteException {
    // Synchronize the block to ensure thread safety for the put operation.
    synchronized (this) {
      store.put(key, value);
      System.out.println("PUT request at " + System.currentTimeMillis() + " for key: " + key);
      return "PUT OK";
    }
  }

  @Override
  public String get(String key) throws RemoteException {
    // Not synchronized  because there can be multiple get operations occurring simultaneously.
    String value = store.get(key);
    System.out.println("GET request at " + System.currentTimeMillis() + " for key: " + key);
    return value != null ? "GET OK: " + value : "GET ERROR: Key not found";
  }

  @Override
  public String delete(String key) throws RemoteException {
    synchronized (this) {
      String response;
      if (store.remove(key) != null) {
        response = "DELETE OK";
      } else {
        response = "DELETE ERROR: Key not found";
      }
      System.out.println("DELETE request at " + System.currentTimeMillis() + " for key: " + key);
      return response;
    }
  }

  @Override
  public boolean prepare(String operation, String key, String value) throws RemoteException {
    System.out.println("PREPARE request at " + System.currentTimeMillis() + " for operation: " + operation + ", key: " + key + ", value: " + value);
    synchronized (this) {
      if (operation.equals("PUT")) {
        prepareStore.put(key, value);
      } else if (operation.equals("DELETE")) {
        prepareStore.remove(key);
      }
      pendingOperation = operation;
      pendingKey = key;
      pendingValue = value;
      return true; // In real word scenario, this is dependent on the readiness of the store.
    }
  }

  @Override
  public void commit(String operation, String key, String value) throws RemoteException {
    System.out.println("COMMIT request at " + System.currentTimeMillis() + " for operation: " + operation + ", key: " + key + ", value: " + value);
    synchronized (this) {
      if (operation.equals("PUT")) {
        store.put(key, value);
      } else if (operation.equals("DELETE")) {
        store.remove(key);
      }
      pendingOperation = null;
      pendingKey = null;
      pendingValue = null;

    }
  }

  @Override
  public void abort(String operation, String key, String value) throws RemoteException {
    System.out.println("ABORT request at " + System.currentTimeMillis() + " for operation: " + operation + ", key: " + key + ", value: " + value);
    synchronized (this) {
      if (operation.equals("PUT")) {
        prepareStore.remove(key);
      } else if (operation.equals("DELETE")) {
        prepareStore.remove(key);
      }
      pendingOperation = null;
      pendingKey = null;
      pendingValue = null;
    }
  }
}
