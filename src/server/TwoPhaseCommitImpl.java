package server;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class TwoPhaseCommitImpl {

  private List<KeyValueStoreInterface> replicas;

  public TwoPhaseCommitImpl(List<KeyValueStoreInterface> replicas) {
    this.replicas = replicas;
  }

  public List<KeyValueStoreInterface> getReplicas() {
    return replicas;
  }

  public boolean performTwoPhaseCommit(String operation, String key, String value) {
    AtomicBoolean success = new AtomicBoolean(true);
    System.out.println("Performing Two-Phase Commit for operation: " + operation + ", key: " + key + ", value: " + value);
    // Phase 1: Prepare phase (send prepare messages)
    CountDownLatch prepareLatch = new CountDownLatch(replicas.size());
    for (KeyValueStoreInterface replica : replicas) {
      new Thread(() -> {
        try {
          boolean prepareResponse = replica.prepare(operation, key, value);
          if (!prepareResponse) {
            success.set(false);
          }
        } catch (Exception e) {
          e.printStackTrace();
          System.err.println("Exception during prepare: " + e);
          success.set(false);
        } finally {
          prepareLatch.countDown();
        }
      }).start();
    }

    try {
      prepareLatch.await();
    } catch (InterruptedException e) {
      System.err.println("Exception during prepare latch await: " + e);
      success.set(false);
      Thread.currentThread().interrupt();
    }

    if (success.get()) {
      // Phase 2: Commit phase (send commit messages)
      CountDownLatch commitLatch = new CountDownLatch(replicas.size());
      for (KeyValueStoreInterface replica : replicas) {
        new Thread(() -> {
          try {
            replica.commit(operation, key, value);
          } catch (Exception e) {
            System.err.println("Exception during commit: " + e);
            success.set(false);
          } finally {
            commitLatch.countDown();
          }
        }).start();
      }

      try {
        commitLatch.await();
      } catch (InterruptedException e) {
        System.err.println("Exception during commit latch await: " + e);
        success.set(false);
        Thread.currentThread().interrupt();
      }
    } else {
      // Abort transaction if prepare phase failed
      abortTransaction(operation, key, value);
    }

    return success.get();
  }

  private void abortTransaction(String operation, String key, String value) {
    System.out.println("Aborting transaction for operation: " + operation + ", key: " + key + ", value: " + value);
    for (KeyValueStoreInterface replica : replicas) {
      new Thread(() -> {
        try {
          replica.abort(operation, key, value);
          System.out.println("Abort successful for replica");
        } catch (Exception e) {
          System.err.println("Exception during abort: " + e);
        }
      }).start();
    }
  }
}
