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
COPY --from=build /app/out ./

EXPOSE 8081

# Set the entry point
ENTRYPOINT ["dotnet", "MyCompany.MyShop.Backend.dll"]
