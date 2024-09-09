package server;

import java.util.concurrent.*;
public class TimeoutThreadPool {
  private final ThreadPoolExecutor executor;

  public TimeoutThreadPool(int poolSize, int timeoutSeconds) {
    // Initialize ThreadPoolExecutor with specified parameters
    this.executor = new ThreadPoolExecutor(
            poolSize,           // Core pool size
            poolSize,           // Maximum pool size
            timeoutSeconds,     // Thread idle timeout
            TimeUnit.SECONDS,   // Timeout unit
            new ArrayBlockingQueue<>(100), // Work queue to hold tasks
            new ThreadPoolExecutor.CallerRunsPolicy()  // Policy for handling tasks beyond capacity
    );
    // Allow core threads to time out if idle
    this.executor.allowCoreThreadTimeOut(true);
  }

  // Submits a task to the thread pool for execution.
  public void submitTask(Runnable task) {
    this.executor.submit(task);
  }

  // Initiates an orderly shutdown of the thread pool. Waits up to 25 seconds for tasks to complete.
  // If tasks do not complete in time, forcefully shuts down the executor.
  public void shutdown() {
    this.executor.shutdown();
    try {
      if (!this.executor.awaitTermination(25, TimeUnit.SECONDS)) {
        this.executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      this.executor.shutdownNow();
      Thread.currentThread().interrupt(); // Preserve interrupt status.
    }
  }
}
