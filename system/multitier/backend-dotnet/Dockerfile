# syntax=docker/dockerfile:1.4
# Use the official .NET SDK image for build
FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build
WORKDIR /app

# Copy csproj and restore as distinct layers
COPY *.csproj ./
RUN --mount=type=cache,target=/root/.nuget/packages dotnet restore

# Copy everything else and build
COPY . ./
RUN dotnet publish MyCompany.MyShop.Backend.csproj -c Release -o out

# Use the official ASP.NET runtime image for the app
FROM mcr.microsoft.com/dotnet/aspnet:8.0
WORKDIR /app

# Install curl for HEALTHCHECK probes (~7 MB)
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

COPY --from=build --chown=app:app /app/out ./
USER app

EXPOSE 8081
HEALTHCHECK --interval=10s --timeout=3s --start-period=20s --retries=5 \
  CMD curl -fsS http://localhost:8081/health || exit 1

# Set the entry point
ENTRYPOINT ["dotnet", "MyCompany.MyShop.Backend.dll"]
