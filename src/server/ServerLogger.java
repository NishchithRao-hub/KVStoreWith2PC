package server;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ServerLogger {
  // The log file where all server logs will be written.
  private static final String LOG_FILE = "server.log";

  // Logs a message related to TCP communication.
  public static void logTCP(String message) {
    log("TCP", message);
  }

  // Logs a message related to UDP communication.
  public static void logUDP(String message) {
    log("UDP", message);
  }

  // Logs a message related to RMI Client communication.
  public static void logRMIServer(String message) {
    log("RMI Client", message);
  }

  // Logs an error message and exception stack trace related to TCP communication.
  public static void logTCPError(String message, Exception e) {
    logError("TCP", message, e);
  }

  // Logs an error message and exception stack trace related to UDP communication.
  public static void logUDPError(String message, Exception e) {
    logError("UDP", message, e);
  }

  // Logs an error message and exception stack trace related to RMI client communication.
  public static void logRMIServerError(String message, Exception e) {
    logError("RMI Server", message, e);
  }

  // Logs a general message with a specified protocol (TCP/UDP).
  private static void log(String protocol, String message) {
    try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, true))) {
      String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
      writer.println(timeStamp + " [" + protocol + "] " + message);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // Logs an error message and stack trace with a specified protocol (TCP/UDP).
  private static void logError(String protocol, String message, Exception e) {
    try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, true))) {
      String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
      writer.println(timeStamp + " [" + protocol + " ERROR] " + message);
      e.printStackTrace(writer);
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }
}
