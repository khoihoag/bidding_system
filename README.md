# Bidding System

Java 21 real-time auction application with a JavaFX client and TCP/JSON server backed by MySQL and Hibernate.

## Modules

- `system/client` — JavaFX UI (`com.bidding.App`)
- `system/server` — Socket server on port `8080` (`com.bidding.server.network.ServerMain`)

## Requirements

- JDK 21
- Maven 3.9+
- MySQL (configure `system/server/src/main/resources/hibernate.cfg.xml`)

## Build & Test

```bash
mvn clean test
```

## Run

Start the server:

```bash
mvn exec:java -Dexec.mainClass="com.bidding.server.network.ServerMain"
```

Start the client:

```bash
mvn javafx:run
```

## Architecture

```
Client (JavaFX)  --TCP/JSON-->  ClientHandler  -->  Controllers  -->  Services  -->  Repositories  -->  MySQL
```
