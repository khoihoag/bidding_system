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

The server reads MySQL defaults from `system/server/src/main/resources/hibernate.cfg.xml`
(`jdbc:mysql://localhost:3306/bidding_system`, user `root`, password `123456`).
Override them when your local MySQL account is different:

```powershell
$env:DB_PASSWORD = "your_mysql_password"
mvn exec:java -Dexec.mainClass="com.bidding.server.network.ServerMain"
```

You can also use JVM properties:

```powershell
mvn exec:java -Dexec.mainClass="com.bidding.server.network.ServerMain" -Ddb.username=root -Ddb.password=your_mysql_password
```

Start the client:

```bash
mvn javafx:run
```

## Architecture

```
Client (JavaFX)  --TCP/JSON-->  ClientHandler  -->  Controllers  -->  Services  -->  Repositories  -->  MySQL
```
