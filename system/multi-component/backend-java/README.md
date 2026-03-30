# Backend Service

This is the backend API service for the Starter, built with Java Spring Boot.

## Features

- RESTful API endpoints
- Echo endpoint for health checks
- Todo endpoint that proxies to external API
- CORS enabled for frontend communication

## Endpoints

- `GET /api/echo` - Returns "Echo" for health check
- `GET /api/todos/{id}` - Fetches a todo item by ID


## Instructions

```shell
cd backend
```
Check that you have Powershell 7

```shell
$PSVersionTable.PSVersion
```

Ensure you have JDK 21 installed

```shell
java -version
```

Check that JAVA_HOME is set correctly & points to your JDK 21 installation

```shell
echo $env:JAVA_HOME
```

Ensure you have Gradle 8.14 installed

```shell
./gradlew --version
```

## Building

```shell
./gradlew build
```

## Running Locally

```shell
./gradlew bootRun
```

The service will start on port 8081.

## Docker

Create network:

```shell
docker network create app-network
```

Build the Docker image:

```shell
docker build -t backend .
```

Run the container on the network:

```shell
docker run -d --name backend --network hero-network -p 8081:8081 backend
```

## Configuration

Configuration can be modified in `src/main/resources/application.yml`:

- `server.port` - Server port (default: 8081)
- `todos.api.host` - External todos API host (default: https://jsonplaceholder.typicode.com)