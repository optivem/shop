# syntax=docker/dockerfile:1.4
# Use the official .NET SDK image for build
FROM mcr.microsoft.com/dotnet/sdk:8.0@sha256:1b955f179b02df4f0eb5fadedaa898cbe44d620a64aab52eec72c8e91d1355b5 AS build
WORKDIR /app
ENV NUGET_PACKAGES=/root/.nuget/packages

# Copy csproj + central package management files (if present) and restore as distinct layer
COPY *.csproj global.json* Directory.Packages.props* ./
RUN --mount=type=cache,target=/root/.nuget/packages dotnet restore

# Copy everything else and build
COPY . ./
RUN dotnet publish MyCompany.MyShop.Backend.csproj -c Release -o out

# Use the official ASP.NET runtime image for the app
FROM mcr.microsoft.com/dotnet/aspnet:8.0@sha256:dccacae9e0e103b5d3ba5fbe442dd8d3316b990cd8f2ec9be0a0ec9db0c0e520
WORKDIR /app

# Install curl for HEALTHCHECK probes (~7 MB)
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

COPY --from=build --chown=app:app /app/out ./
USER app

EXPOSE 8081
HEALTHCHECK --interval=10s --timeout=3s --start-period=20s --retries=5 \
  CMD curl -fsS http://localhost:8081/livez || exit 1

# Set the entry point
ENTRYPOINT ["dotnet", "MyCompany.MyShop.Backend.dll"]
