# READ ME

## Goal:

Replicating a Key-Value Store Server across 5 distinct servers, using a single instance of a Key-Value Store server. 
To increase Server bandwidth and ensure availability, we are replicating the key-value store at each of 5 different 
instances of our servers. The clients will be able to contact any of the five KV replica servers instead of a 
single server and get consistent data back from any of the replicas (in the case of GETs). The client will also be
able to issue PUT operations and DELETE operations to any of the five replicas.

On PUT or DELETE operations we ensure each of the replicated KV stores at each replica is
consistent. For this, we are implementing a two-phase protocol for updates. We will assume no
servers will fail such that 2 Phase Commit will not stall, although we will defensively code
our 2PC protocol with timeouts to be sure. Consequently, whenever a client issues a PUT or a
DELETE to *any* server replica, that receiving replica will ensure the updates have been received (via
ACKs) and commited (via Go messages with accompanying ACKs).

#### Project structure

Before packaging out application, given below is the project structure which has `client` and `server` packages.

```bash
src
├── README.md
├── client
│   ├── ClientLogger.java
│   ├── TCPClient.java
│   └── UDPClient.java
│   └── KeyValueStoreClient.java
│   └── ClientOperations.java
│   └── ConcurrentOperationsTest.java
├── server
│   ├── KeyValueStoreInterface.java
│   ├── KeyValueStoreImpl.java
│   ├── KeyValueStoreServer.java
│   ├── KeyValueStoreWith2PC.java
│   ├── ReplicaKeyValueStoreServer.java
│   ├── TwoPhaseCommitImpl.java
│   ├── RequestProcessor.java
│   ├── ServerLogger.java
│   ├── TCPServer.java
│   ├── UDPServer.java
│   └── TimeoutThreadPool.java

2 directories, 18 files
```

#### Project structure

Before you start, ensure you have the following software installed on your machine:

1. Java Development Kit (JDK) 11 or higher:

    * Download and install JDK from [Oracle](https://www.oracle.com/java/technologies/downloads/#java11)

2. Docker Desktop:

    * Docker Desktop is required to build and run Docker containers.
    * Download and install Docker Desktop from [Docker's official website](https://www.docker.com/products/docker-desktop/).

3. Git:

    * Git is useful for version control and cloning repositories.
    * Download and install Git from [Git's official website](https://www.git-scm.com/downloads).

#### Note
Make sure you are in the `src` directory when running all the commands. 

### Note
Please make necessary adjustments such as changing hostname and port-number according 
to your system configurations in all the .sh files. 

***
#### Running the servers directly from terminal:

1. Open a terminal and make sure it's in the `src` directory. Ex: `cd src`.
2. Compile the code using `javac server/*.java client/*.java`

3. Run the Replica Key Value Server using `java server.ReplicaKeyValueStoreServer`
4. Open a new terminal pointing to the `src` directory. 
5. Run the TCP server using `java server.TCPServer <tcp-port-number> <replica-registry-urls>`.
    * Example: `java server.TCPServer 8888 localhost:1099 localhost:1100 localhost:1101 localhost:1102 localhost:1103
      `.
    * Here, `<replica-registry-url>` consists of 2 parts-hostname/ip-address of the system followed by 
    * ReplicaKeyValueStoreServer's port numbers, here `1099`,`1100`,`1101`,`1102`,`1103`. 
   
6. Open a new terminal pointing to the `src` directory.
7. Run the UDP server using `java server.UDPServer <udp-port-number> <replica-registry-urls>`.
    * Example: `java server.UDPServer 9999 localhost:1099 localhost:1100 localhost:1101 localhost:1102 localhost:1103`.
    * Here, `<replica-registry-url>` consists of 2 parts-hostname/ip-address of the system followed by
    * ReplicaKeyValueStoreServer's port numbers, here default `1099`,`1100`,`1101`,`1102`,`1103`.

#### Sending requests from the client:

1. Let's pre-populate the key-value store using the client:
    * `java client.KeyValueStoreClient <hostname> <operation> <key> <value> <concurrent-requests>`
    * Example: `java client.KeyValueStoreClient localhost PUT dummy_key dummy_value 1`.
    * `<hostname>` can be the name of your host or ipv4-address of your Ethernet or Wi-Fi.
    * `<port-number>` is the port on which the server is running
    * `<key>` here is key name.
    * `<value>` here is value held by that key.
    * `<concurrent-requests` is the number of requests you want to send. (Minimum=1)
    * Doing the above, pre-populates the key-value store with 5 values - key1, key2 and so on. 
    * You can perform get/delete operations from any client to verify its correctness. 

2. To perform 5 PUT,5 GET and 5 DELETE operations, run the following script file:
    * Open your terminal and change the directory to the `src` directory. 
    * Run the following command: `java client.ClientOperations`
   
3. To test sending multiple concurrent requests to the server:
    * Open your terminal and change the directory to the `src` directory.
    * Run the following command: `java client.ConcurrentOperationsTest`

4. Insert a key-value into the key-value store:
    * `java client.TCPClient <hostname> <port-number> <operation> <key> <value> <concurrent-requests>`
    * Example: `java client.TCPClient localhost 8888 PUT key10 value10 1`.
    * The <concurrent-requests> is optional.

5. Retrieve a value from the key-value store:
    * `java client.TCPClient <hostname> <port-number> <operation> <key> <value> <concurrent-requests>`
    * Example: `java client.TCPClient localhost 8888 GET key10 0 1`.
    * Specify <value> as `0` for this operation as we need only key. 

6. Delete a key from the key-value store:
    * `java client.TCPClient <hostname> <port-number> <operation> <key> <value> <concurrent-requests>`
    * Example: `java client.TCPClient localhost 8888 DELETE key10 0 1.`
    * Specify <value> as `0` for this operation as we need only key.

7. Follow the same above steps even in the case of UDP:
    * Example: `java client.UDPClient <hostname> <port-number> <operation> <key> <value> <concurrent-requests>`.

***

#### Logging

All error messages and requests are recorded in the respective client.log and server.log files. This helps in tracking the operations performed and debugging issues if they arise.

### Note
Do not forget to change the permission of .sh files to executable using chmod +x *.sh.

With this, one should be able to compile, run, and test both the functioning of replica stores, 
servers and see the functioning of 2 Phase Commit Protocol both server and client side locally.
